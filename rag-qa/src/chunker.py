"""
文本分块器 — 将长文档拆分为适合检索的语义块

设计要点：
  - 自定义递归分块（零外部依赖），逻辑等同 LangChain RecursiveCharacterTextSplitter
  - 递归分隔符优先级：段落 > 行 > 句子 > 子句 > 字符
  - chunk_overlap 确保跨块信息不丢失
  - 对中文场景优化分隔符

RAG 中分块策略的影响：
  - 块太大 → 检索精度下降，噪声多
  - 块太小 → 语义不完整，上下文断裂
  - overlap 太小 → 关键信息可能被切断
  - overlap 太大 → 冗余信息多，浪费 token
"""


SEPARATORS = [
    "\n\n",     # 段落
    "\n",       # 行
    "。",       # 中文句号
    "！",       # 中文感叹号
    "？",       # 中文问号
    "；",       # 中文分号
    "，",       # 中文逗号
    ". ",       # 英文句号+空格
    " ",        # 空格
    "",         # 字符级
]


def _split_by_separator(text: str, separator: str) -> list[str]:
    """按分隔符切分，保留分隔符在对应片段末尾"""
    if not separator:
        return list(text)  # 字符级切分

    parts = text.split(separator)
    # 除最后一段外，每段末尾追加分隔符
    result = []
    for i, part in enumerate(parts):
        if i < len(parts) - 1:
            result.append(part + separator)
        elif part:
            result.append(part)
    return result


def _recursive_split(
    text: str,
    separators: list[str],
    chunk_size: int,
    chunk_overlap: int,
) -> list[str]:
    """
    递归分块核心算法：
    1. 按当前分隔符切分
    2. 将小块合并到 chunk_size 上限
    3. 超出的大块递归用下一级分隔符继续切
    4. 相邻块之间保留 overlap
    """
    # 选中当前分隔符列
    sep_idx = 0
    for i, sep in enumerate(separators):
        if sep in text:
            sep_idx = i
            break
    else:
        sep_idx = len(separators) - 1  # 兜底用字符级

    current_sep = separators[sep_idx]
    next_seps = separators[sep_idx + 1 :] if sep_idx + 1 < len(separators) else []

    splits = _split_by_separator(text, current_sep)

    chunks: list[str] = []
    buf = ""

    for split in splits:
        if len(buf) + len(split) <= chunk_size:
            buf += split
        else:
            # 当前 buf 非空则提交
            if buf:
                chunks.append(buf)

            # 当前 split 本身超出 chunk_size → 递归切分
            if len(split) > chunk_size:
                if next_seps:
                    chunks.extend(
                        _recursive_split(split, next_seps, chunk_size, chunk_overlap)
                    )
                else:
                    # 无更细分隔符，强制按字符切
                    chunks.extend(
                        [split[i : i + chunk_size] for i in range(0, len(split), chunk_size)]
                    )
                buf = ""
            else:
                buf = split

    if buf:
        chunks.append(buf)

    # 合并过短的块 & 创建 overlap
    return _merge_with_overlap(chunks, chunk_size, chunk_overlap)


def _merge_with_overlap(
    chunks: list[str], chunk_size: int, chunk_overlap: int
) -> list[str]:
    """合并过小的 chunk，并在相邻块之间添加重叠"""
    if not chunks:
        return chunks

    # 合并短块
    merged: list[str] = []
    buf = ""
    for chunk in chunks:
        if len(buf) + len(chunk) <= chunk_size:
            buf += chunk
        else:
            if buf:
                merged.append(buf)
            buf = chunk
    if buf:
        merged.append(buf)

    # 无需 overlap 或只有一个块
    if chunk_overlap <= 0 or len(merged) <= 1:
        return merged

    # 为相邻块添加重叠：从前一个块末尾截取 overlap 拼到当前块开头
    result = [merged[0]]
    for i in range(1, len(merged)):
        prev = merged[i - 1]
        curr = merged[i]
        # 取前一块末尾 overlap 字符
        overlap_text = prev[-chunk_overlap:] if len(prev) > chunk_overlap else prev
        result.append(overlap_text + curr)

    return result


def chunk_documents(
    documents: list[dict],
    chunk_size: int = 500,
    chunk_overlap: int = 80,
) -> list[dict]:
    """
    对文档列表进行分块

    Args:
        documents: [{"name": "文件名", "content": "文本"}, ...]
        chunk_size: 每块最大字符数
        chunk_overlap: 相邻块重叠字符数

    Returns:
        [{"id": "唯一ID", "text": "块文本", "source": "来源文件"}, ...]
    """
    chunks: list[dict] = []

    for doc in documents:
        texts = _recursive_split(
            doc["content"], SEPARATORS, chunk_size, chunk_overlap
        )
        for i, text in enumerate(texts):
            chunks.append({
                "id": f"{doc['name']}_chunk_{i:04d}",
                "text": text.strip(),
                "source": doc["name"],
            })

    return chunks
