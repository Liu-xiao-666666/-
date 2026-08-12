"""
生成器 — 将检索结果 + 用户问题组装为 Prompt，调用 LLM 生成答案

设计要点：
  - System Prompt 要求 AI 严格基于给定上下文回答，不编造信息
  - 来源引用：要求 AI 标注答案依据哪些文档
  - 不知道就说不知道：避免幻觉的兜底策略
  - 兼容 OpenAI SDK 格式调用 DeepSeek API
"""

from openai import OpenAI


class Generator:
    """基于检索上下文的 LLM 答案生成器"""

    SYSTEM_PROMPT = """你是一个基于知识库的智能问答助手。

规则：
1. 严格基于【参考资料】中的内容回答问题，不要使用你自己的知识
2. 如果参考资料中没有相关信息，直接回答"抱歉，知识库中没有找到相关信息"
3. 回答时引用具体的来源文件名
4. 回答简洁清晰，控制在 300 字以内
5. 如果参考资料中有多条相关但不同角度的信息，请综合后给出答案"""

    def __init__(
        self,
        api_key: str,
        base_url: str = "https://api.deepseek.com/v1",
        model: str = "deepseek-chat",
    ):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model

    def generate(
        self,
        query: str,
        contexts: list[dict],
        max_tokens: int = 800,
        temperature: float = 0.3,
    ) -> str:
        """
        基于检索上下文生成答案

        Args:
            query: 用户问题
            contexts: 检索到的相关文档块
            max_tokens: 最大生成 token 数
            temperature: 生成温度（低温度减少幻觉）

        Returns:
            LLM 生成的回答文本
        """
        # 组装上下文
        context_text = self._build_context(contexts)

        # 组装用户消息
        user_message = f"""【参考资料】
{context_text}

【用户问题】
{query}

请基于参考资料回答问题："""

        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": self.SYSTEM_PROMPT},
                    {"role": "user", "content": user_message},
                ],
                max_tokens=max_tokens,
                temperature=temperature,
            )
            return response.choices[0].message.content or ""
        except Exception as e:
            return f"生成失败: {e}"

    def _build_context(self, contexts: list[dict]) -> str:
        """将检索结果拼接为上下文文本"""
        parts: list[str] = []
        for i, ctx in enumerate(contexts, 1):
            parts.append(
                f"[参考{i}] 来源: {ctx['source']} (相关度: {ctx['score']:.2f})\n"
                f"{ctx['text']}"
            )
        return "\n\n".join(parts)
