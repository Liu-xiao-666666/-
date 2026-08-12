# RAG 智能文档问答系统

> 基于 LangChain + ChromaDB + DeepSeek 的 RAG (Retrieval-Augmented Generation) 实战项目

## 项目简介

一个完整的 RAG（检索增强生成）文档问答系统，实现从文档加载、文本分块、向量化、存储检索到 LLM 生成答案的全链路。

**核心价值**：让大模型基于本地知识库回答问题，而不是依赖训练数据中的"记忆"——解决 LLM 幻觉问题，确保回答可溯源。

## RAG 架构

```
                    ┌─────────────┐
                    │  用户提问    │
                    └──────┬──────┘
                           │
              ┌────────────▼────────────┐
              │   1. Query Embedding    │
              │   sentence-transformers │
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │   2. 向量相似度检索      │
              │   ChromaDB (cosine)     │
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │   3. 相似度过滤 (≥0.3)  │
              │   取 Top-K 相关文档块    │
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │   4. Prompt 组装        │
              │   上下文 + 问题 + 指令   │
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │   5. LLM 生成回答        │
              │   DeepSeek Chat API      │
              └────────────┬────────────┘
                           │
                    ┌──────▼──────┐
                    │  结构化回答  │
                    │  + 来源引用  │
                    └─────────────┘

离线阶段（ingest）:
  文档(.txt/.md/.pdf) → 加载 → 分块(500字/块, 80字重叠)
  → Embedding(384维) → ChromaDB 存储

在线阶段（ask）:
  用户问题 → Embedding → 检索 Top-K → LLM 生成 → 答案
```

## 技术栈

| 组件 | 技术 | 说明 |
|------|------|------|
| 文档加载 | pypdf / Python 原生 | 支持 TXT、Markdown、PDF |
| 文本分块 | LangChain RecursiveCharacterTextSplitter | 递归分块，中文优化分隔符 |
| 向量化 | sentence-transformers (all-MiniLM-L6-v2) | 本地模型，384 维，~80MB |
| 向量存储 | ChromaDB | 本地持久化，余弦相似度 |
| 答案生成 | DeepSeek Chat API | GPT 兼容接口 |

## 快速开始

### 1. 安装依赖

```bash
cd rag-qa
pip install -r requirements.txt
```

### 2. 配置 API Key

编辑 `config.py`，填入你的 DeepSeek API Key：

```python
DEEPSEEK_API_KEY = "sk-your-key-here"
```

### 3. 准备知识库文档

将文档（.txt / .md / .pdf）放入 `sample_data/` 目录。已内置一份外卖知识库示例文档。

### 4. 构建知识库（Ingest）

```bash
python main.py ingest
```

输出示例：
```
==================================================
RAG 文档问答系统 — 知识库构建
==================================================

[1/4] 加载文档 ...
  ✓ food_knowledge.txt (4820 字符)

[2/4] 文本分块 (chunk_size=500, overlap=80) ...
  ✓ 共生成 14 个文本块

[3/4] 向量化 ...
  加载嵌入模型: all-MiniLM-L6-v2 ... ✓ (维度=384)
  ✓ 已生成 14 个向量

[4/4] 存入向量数据库 ...
  ✓ 已入库 14 个文档块
  ✓ 知识库构建完成，共 14 条记录
```

### 5. 提问

```bash
python main.py ask "配送超时了怎么办？"
```

### 6. 其他命令

```bash
python main.py search "退款"    # 只看检索结果（不调用 LLM）
python main.py stats            # 查看知识库统计
```

## 项目结构

```
rag-qa/
├── config.py              # 全局配置（API Key、分块参数、检索参数）
├── main.py                # CLI 入口（ingest / ask / search / stats）
├── requirements.txt       # Python 依赖
├── README.md              # 本文件
├── src/
│   ├── loader.py          # 文档加载（TXT / MD / PDF）
│   ├── chunker.py         # 文本分块（RecursiveCharacterTextSplitter）
│   ├── embedder.py        # 向量化（sentence-transformers）
│   ├── vector_store.py    # ChromaDB 封装
│   ├── retriever.py       # 检索器（查询向量化 + 相似度搜索 + 过滤）
│   └── generator.py       # LLM 生成器（DeepSeek API）
└── sample_data/
    └── food_knowledge.txt # 示例知识库（外卖 FAQ）
```

## 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 嵌入模型 | 本地 sentence-transformers | 零成本、隐私、展示 Embedding 原理理解 |
| 向量数据库 | ChromaDB | 轻量、零配置、Python 原生 |
| LLM | DeepSeek Chat | 性价比高、中文优秀、OpenAI 兼容 |
| 分块策略 | 500 字 + 80 重叠 | 在精度和效率之间的平衡 |
| 检索策略 | Top-4 + 阈值 0.3 | 过滤低质量结果，避免误导 LLM |
