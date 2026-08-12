"""
检索器 — 查询 → 向量化 → 检索 → 过滤 → 返回 Top-K 上下文

设计要点：
  - 封装完整检索链路
  - 相似度阈值过滤：低质量结果直接丢弃，避免误导 LLM
  - 返回结构化检索结果，含来源引用
"""

from .embedder import Embedder
from .vector_store import VectorStore


class Retriever:
    """RAG 检索器"""

    def __init__(self, embedder: Embedder, vector_store: VectorStore):
        self.embedder = embedder
        self.store = vector_store

    def retrieve(
        self,
        query: str,
        top_k: int = 4,
        threshold: float = 0.3,
    ) -> list[dict]:
        """
        检索与查询最相关的文档块

        Args:
            query: 用户查询文本
            top_k: 返回最相似的 K 个结果
            threshold: 相似度阈值，低于此值的结果丢弃

        Returns:
            [{"id": ..., "text": ..., "source": ..., "score": ...}, ...]
        """
        # Step 1: 查询向量化
        query_vec = self.embedder.encode_single(query)

        # Step 2: 向量检索
        raw_results = self.store.search(query_vec, top_k=top_k * 2)  # 多取一些供过滤

        # Step 3: 相似度过滤
        filtered = [r for r in raw_results if r["score"] >= threshold]

        # Step 4: 截取 Top-K
        return filtered[:top_k]
