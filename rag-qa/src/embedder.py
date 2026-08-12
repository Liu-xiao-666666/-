"""
向量化模块 — 将文本转换为语义向量

提供两种模式（自动降级）：
  1. sentence-transformers: 神经网络 Embedding，384维
  2. TF-IDF + SVD: 经典机器学习方案，零下载，纯本地计算

设计要点：
  - 优先尝试 sentence-transformers，失败快速降级到 TF-IDF
  - TF-IDF 模型持久化到磁盘（joblib），ingest 和 search 共享同一向量空间
  - 两种方案对外接口完全一致
"""

import pickle
from pathlib import Path

import numpy as np


class Embedder:
    """文本向量化器（自动选择可用后端，支持模型持久化）"""

    def __init__(self, model_name: str = "all-MiniLM-L6-v2", cache_dir: str = ""):
        self.model_name = model_name
        self.cache_dir = Path(cache_dir) if cache_dir else None
        self.dimension = 0
        self.backend = "tfidf"
        self._tfidf = None
        self._svd = None
        self._model = None  # sentence-transformers model

        # 尝试加载 sentence-transformers
        try:
            self._init_sentence_transformer()
        except Exception:
            # TF-IDF 在 fit() 时初始化
            pass

    def _init_sentence_transformer(self):
        """尝试加载神经网络模型（网络不可用时快速降级，不产生噪音）"""
        import os
        import warnings
        import logging as _logging

        os.environ["HF_HUB_DOWNLOAD_TIMEOUT"] = "2"
        for _name in ("sentence_transformers", "huggingface_hub", "httpx", "urllib3", "transformers"):
            _logging.getLogger(_name).setLevel(_logging.ERROR)

        # 先做快速连通性检测（3 秒超时），不可达则直接降级
        if not self._hf_reachable():
            raise RuntimeError("HuggingFace 不可达")

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            for endpoint in ["https://hf-mirror.com", "https://huggingface.co"]:
                try:
                    os.environ["HF_ENDPOINT"] = endpoint
                    from sentence_transformers import SentenceTransformer
                    model = SentenceTransformer(self.model_name, trust_remote_code=False)
                    self._model = model
                    self.dimension = model.get_sentence_embedding_dimension()
                    self.backend = "sentence-transformers"
                    print(f"  [嵌入] sentence-transformers ({self.dimension}维)")
                    return
                except Exception:
                    continue
        raise RuntimeError("不可用")

    @staticmethod
    def _hf_reachable(timeout: float = 3.0) -> bool:
        """检测 HuggingFace 镜像是否可达"""
        import urllib.request
        for host in ("https://hf-mirror.com", "https://huggingface.co"):
            try:
                urllib.request.urlopen(host, timeout=timeout)
                return True
            except Exception:
                continue
        return False

    # ===== TF-IDF =====

    def fit(self, texts: list[str]):
        """在全部文本上训练 TF-IDF + SVD（仅在 TF-IDF 模式下调用）"""
        if self.backend == "sentence-transformers":
            return

        from sklearn.feature_extraction.text import TfidfVectorizer
        from sklearn.decomposition import TruncatedSVD

        self._tfidf = TfidfVectorizer(
            max_features=2000, analyzer="char_wb", ngram_range=(2, 4), min_df=1,
        )
        tfidf_matrix = self._tfidf.fit_transform(texts)

        n_comp = min(128, tfidf_matrix.shape[1] - 1, tfidf_matrix.shape[0] - 1)
        n_comp = max(16, n_comp)

        self._svd = TruncatedSVD(n_components=n_comp, random_state=42)
        self._svd.fit(tfidf_matrix)
        self.dimension = n_comp
        print(f"  [嵌入] TF-IDF + SVD ({self.dimension}维)")

    def encode(self, texts: list[str]) -> list[list[float]]:
        """将文本列表转换为向量列表"""
        if self.backend == "sentence-transformers" and self._model:
            embs = self._model.encode(texts, normalize_embeddings=True)
            return [e.tolist() for e in embs]
        return self._encode_tfidf(texts)

    def encode_single(self, text: str) -> list[float]:
        return self.encode([text])[0]

    def _encode_tfidf(self, texts: list[str]) -> list[list[float]]:
        if self._tfidf is None or self._svd is None:
            raise RuntimeError("TF-IDF 模型未训练，请先调用 fit()")
        tfidf = self._tfidf.transform(texts)
        dense = self._svd.transform(tfidf)
        norms = np.linalg.norm(dense, axis=1, keepdims=True)
        norms[norms == 0] = 1.0
        normalized = dense / norms
        return [v.tolist() for v in normalized]

    # ===== 持久化 =====

    def save(self, path: str):
        """保存 TF-IDF 模型到磁盘"""
        if self.backend != "tfidf":
            return
        p = Path(path)
        p.parent.mkdir(parents=True, exist_ok=True)
        with open(p, "wb") as f:
            pickle.dump({"tfidf": self._tfidf, "svd": self._svd, "dim": self.dimension}, f)

    def load(self, path: str):
        """从磁盘加载 TF-IDF 模型"""
        p = Path(path)
        if not p.exists():
            return False
        with open(p, "rb") as f:
            data = pickle.load(f)
        self._tfidf = data["tfidf"]
        self._svd = data["svd"]
        self.dimension = data["dim"]
        self.backend = "tfidf"
        print(f"  [嵌入] 已加载 TF-IDF 模型 ({self.dimension}维)")
        return True
