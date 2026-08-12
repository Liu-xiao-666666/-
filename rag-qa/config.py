"""
RAG 智能文档问答系统 — 全局配置

使用方式：
  - 直接修改此文件中的默认值
  - 或通过环境变量 / .env 文件覆盖（优先级更高）
"""

import os
from pathlib import Path

# 加载 .env 文件（如存在）
try:
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parent / ".env")
except ImportError:
    pass

# ============ 项目路径 ============
BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "sample_data"
PERSIST_DIR = BASE_DIR / "chroma_db"       # ChromaDB 持久化目录

# ============ DeepSeek API（生成用） ============
# 请通过环境变量或 .env 文件配置，切勿硬编码到代码中
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")   # ← 填入你的 API Key 或用 .env
DEEPSEEK_BASE_URL = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1")
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")

# ============ 嵌入模型（本地运行，无需 API） ============
# all-MiniLM-L6-v2: 体积小 (~80MB)、速度快、中文可用
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "all-MiniLM-L6-v2")

# ============ 分块策略 ============
CHUNK_SIZE = 500            # 每块最大字符数
CHUNK_OVERLAP = 80          # 相邻块重叠字符数（防止关键信息被截断）

# ============ 检索策略 ============
TOP_K = 4                   # 每次检索返回最相似的 K 个块
SIMILARITY_THRESHOLD = 0.3  # 相似度阈值（低于此值的结果丢弃）

# ============ 生成策略 ============
MAX_TOKENS = 800
TEMPERATURE = 0.3           # RAG 场景用低温度，减少幻觉
