"""
RAG 智能文档问答系统 — CLI 入口

使用方法：
  python main.py ingest          # 1. 加载文档 → 分块 → 向量化 → 存储
  python main.py ask "问题"      # 2. 提问（自动检索 + 生成回答）
  python main.py search "关键词" # 3. 只看检索结果（不调用 LLM）
  python main.py stats           # 4. 查看知识库统计

示例流程：
  python main.py ingest
  python main.py ask "外卖配送超时了怎么办？"
  python main.py ask "有哪些优惠活动？"
"""

import sys
import io

# Windows 控制台 UTF-8 编码修复
if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

import config
from src.loader import load_documents
from src.chunker import chunk_documents
from src.embedder import Embedder
from src.vector_store import VectorStore
from src.retriever import Retriever
from src.generator import Generator

TFIDF_MODEL_PATH = str(config.PERSIST_DIR / "tfidf_model.pkl")


def cmd_ingest():
    """ingest: 将 sample_data 目录下的文档建立索引"""
    print("=" * 50)
    print("RAG 文档问答系统 — 知识库构建")
    print("=" * 50)

    # Step 1: 加载文档
    print("\n[1/4] 加载文档 ...")
    docs = load_documents(config.DATA_DIR)
    if not docs:
        print(f"  ✗ 未在 {config.DATA_DIR} 找到文档，请放入 .txt / .md / .pdf 文件")
        return
    for d in docs:
        print(f"  ✓ {d['name']} ({len(d['content'])} 字符)")

    # Step 2: 分块
    print(f"\n[2/4] 文本分块 (chunk_size={config.CHUNK_SIZE}, overlap={config.CHUNK_OVERLAP}) ...")
    chunks = chunk_documents(docs, config.CHUNK_SIZE, config.CHUNK_OVERLAP)
    print(f"  ✓ 共生成 {len(chunks)} 个文本块")

    # Step 3: 向量化 + 训练 TF-IDF
    print(f"\n[3/4] 向量化 ...")
    embedder = Embedder(config.EMBEDDING_MODEL)
    texts = [c["text"] for c in chunks]

    if embedder.backend == "tfidf":
        embedder.fit(texts)
        embedder.save(TFIDF_MODEL_PATH)
    embeddings = embedder.encode(texts)
    print(f"  ✓ 已生成 {len(embeddings)} 个向量 (维度={embedder.dimension})")

    # Step 4: 存储
    print(f"\n[4/4] 存入向量数据库 ({config.PERSIST_DIR}) ...")
    store = VectorStore(str(config.PERSIST_DIR), reset=True)
    store.add_chunks(chunks, embeddings)
    print(f"  ✓ 知识库构建完成，共 {store.count()} 条记录")

    print("\n" + "=" * 50)
    print('知识库已就绪！试试: python main.py ask "你的问题"')
    print("=" * 50)


def _get_retriever() -> Retriever:
    """初始化检索器（自动加载/训练 TF-IDF 模型）"""
    embedder = Embedder(config.EMBEDDING_MODEL)

    if embedder.backend == "tfidf":
        # 尝试加载已保存的模型
        if not embedder.load(TFIDF_MODEL_PATH):
            # 模型不存在，用全量文本重新训练
            docs = load_documents(config.DATA_DIR)
            chunks = chunk_documents(docs, config.CHUNK_SIZE, config.CHUNK_OVERLAP)
            texts = [c["text"] for c in chunks]
            embedder.fit(texts)
            embedder.save(TFIDF_MODEL_PATH)

    store = VectorStore(str(config.PERSIST_DIR))
    return Retriever(embedder, store)


def cmd_search(query: str):
    """search: 仅检索，不调用 LLM"""
    print("=" * 50)
    print(f"检索: {query}")
    print("=" * 50)

    retriever = _get_retriever()
    results = retriever.retrieve(query, config.TOP_K, config.SIMILARITY_THRESHOLD)

    if not results:
        print("\n未找到相关文档。")
        return

    print(f"\n找到 {len(results)} 条相关结果:\n")
    for i, r in enumerate(results, 1):
        print(f"--- 结果 {i} | 来源: {r['source']} | 相关度: {r['score']:.4f} ---")
        print(r["text"][:300] + ("..." if len(r["text"]) > 300 else ""))
        print()


def cmd_ask(query: str):
    """ask: 检索 + LLM 生成回答"""
    print("=" * 50)
    print(f"问题: {query}")
    print("=" * 50)

    # Step 1: 检索
    print("\n[检索中...]")
    try:
        retriever = _get_retriever()
        results = retriever.retrieve(query, config.TOP_K, config.SIMILARITY_THRESHOLD)
    except Exception as e:
        print(f"  检索失败: {e}")
        print("  请先运行: python main.py ingest")
        return

    if not results:
        print("  未找到相关文档，无法回答。")
        print("  提示: 尝试先用 python main.py ingest 重建知识库")
        return

    print(f"  找到 {len(results)} 条相关文档块")
    for i, r in enumerate(results, 1):
        print(f"    [{i}] {r['source']} (相关度: {r['score']:.4f})")

    # Step 2: 生成
    print("\n[生成回答中...]")
    generator = Generator(
        api_key=config.DEEPSEEK_API_KEY,
        base_url=config.DEEPSEEK_BASE_URL,
        model=config.DEEPSEEK_MODEL,
    )
    answer = generator.generate(
        query=query,
        contexts=results,
        max_tokens=config.MAX_TOKENS,
        temperature=config.TEMPERATURE,
    )

    print("\n" + "=" * 50)
    print("回答:")
    print("=" * 50)
    print(answer)
    print()


def cmd_stats():
    """stats: 查看知识库信息"""
    try:
        store = VectorStore(str(config.PERSIST_DIR))
        print(f"知识库文档块总数: {store.count()}")
        print(f"持久化目录: {config.PERSIST_DIR}")
        print(f"嵌入模型: {config.EMBEDDING_MODEL}")
        print(f"分块大小: {config.CHUNK_SIZE} (重叠: {config.CHUNK_OVERLAP})")
    except Exception as e:
        print(f"知识库尚未初始化，请先运行: python main.py ingest\n({e})")


def print_usage():
    print("""RAG 智能文档问答系统

用法:
  python main.py ingest           构建知识库（加载文档 → 分块 → 向量化 → 存储）
  python main.py ask <问题>       提问（检索 + AI 生成回答）
  python main.py search <关键词>  检索（只看相关文档，不调用 AI）
  python main.py stats            查看知识库统计信息

示例:
  python main.py ingest
  python main.py ask "如何申请退款？"
  python main.py search "优惠"
  python main.py stats
""")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print_usage()
        sys.exit(0)

    cmd = sys.argv[1].lower()

    if cmd == "ingest":
        cmd_ingest()
    elif cmd == "ask":
        if len(sys.argv) < 3:
            print("请提供问题: python main.py ask \"你的问题\"")
            sys.exit(1)
        cmd_ask(sys.argv[2])
    elif cmd == "search":
        if len(sys.argv) < 3:
            print("请提供关键词: python main.py search \"关键词\"")
            sys.exit(1)
        cmd_search(sys.argv[2])
    elif cmd == "stats":
        cmd_stats()
    else:
        print(f"未知命令: {cmd}")
        print_usage()
        sys.exit(1)
