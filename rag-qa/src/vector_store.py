"""
向量数据库模块 — 基于 ChromaDB 的向量存储与检索

设计要点：
  - ChromaDB: 轻量级本地向量数据库，无需独立服务
  - 支持持久化（重启后数据不丢失）
  - reset=False: 读取已有数据（检索场景）
  - reset=True:  清空重建（ingest 场景）

ChromaDB 在 RAG 中的角色：
  文档 → 分块 → Embedding → [ChromaDB 存储] ← 查询 Embedding → 相似度搜索 → Top-K
"""

import chromadb
from chromadb.config import Settings


class VectorStore:
    """ChromaDB 向量数据库封装"""

    def __init__(self, persist_dir: str, collection_name: str = "rag_docs", reset: bool = False):
        """
        Args:
            persist_dir: 持久化目录路径
            collection_name: 集合名称
            reset: True=清空重建（ingest），False=读取已有数据（search）
        """
        self.client = chromadb.PersistentClient(
            path=persist_dir,
            settings=Settings(anonymized_telemetry=False),
        )

        if reset:
            # 重建：删除旧集合 → 创建新集合
            try:
                self.client.delete_collection(collection_name)
            except Exception:
                pass
            self.collection = self.client.create_collection(
                name=collection_name,
                metadata={"hnsw:space": "cosine"},
            )
        else:
            # 读取：获取已有集合，不存在则报错
            try:
                self.collection = self.client.get_collection(collection_name)
            except Exception:
                raise RuntimeError(
                    f"知识库集合 '{collection_name}' 不存在，请先运行: python main.py ingest"
                )

    def add_chunks(self, chunks: list[dict], embeddings: list[list[float]]):
        """
        批量添加文档块及其向量

        Args:
            chunks: [{"id": ..., "text": ..., "source": ...}, ...]
            embeddings: 对应的向量列表
        """
        self.collection.add(
            ids=[c["id"] for c in chunks],
            embeddings=embeddings,
            documents=[c["text"] for c in chunks],
            metadatas=[{"source": c["source"]} for c in chunks],
        )
        print(f"  已入库 {len(chunks)} 个文档块")

    def search(
        self,
        query_embedding: list[float],
        top_k: int = 4,
    ) -> list[dict]:
        """
        向量相似度检索

        Args:
            query_embedding: 查询向量
            top_k: 返回最相似的 K 个结果

        Returns:
            [{"id": ..., "text": ..., "source": ..., "score": ...}, ...]
        """
        results = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k,
            include=["documents", "metadatas", "distances"],
        )

        items: list[dict] = []
        if not results["ids"] or not results["ids"][0]:
            return items

        for i, doc_id in enumerate(results["ids"][0]):
            items.append({
                "id": doc_id,
                "text": results["documents"][0][i],
                "source": results["metadatas"][0][i].get("source", ""),
                "score": round(1.0 - results["distances"][0][i], 4),
            })

        return items

    def count(self) -> int:
        """返回已存储的文档块数量"""
        return self.collection.count()
