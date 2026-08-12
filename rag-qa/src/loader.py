"""
文档加载器 — 支持 TXT / Markdown / PDF 三种格式

设计要点：
  - 统一接口：所有格式 load() 后返回纯文本字符串
  - PDF 解析使用 pypdf，提取所有页面文本
  - Markdown 保留原始内容（后续分块时处理）
"""

from pathlib import Path


class DocumentLoader:
    """多格式文档加载器"""

    SUPPORTED_SUFFIXES = {".txt", ".md", ".pdf"}

    def __init__(self, file_path: str | Path):
        self.file_path = Path(file_path)
        if not self.file_path.exists():
            raise FileNotFoundError(f"文件不存在: {self.file_path}")
        if self.file_path.suffix.lower() not in self.SUPPORTED_SUFFIXES:
            raise ValueError(f"不支持的文件格式: {self.file_path.suffix}，支持: {self.SUPPORTED_SUFFIXES}")

    def load(self) -> str:
        """加载文档并返回纯文本"""
        suffix = self.file_path.suffix.lower()
        if suffix == ".pdf":
            return self._load_pdf()
        else:
            # txt / md 统一按 UTF-8 读取
            return self.file_path.read_text(encoding="utf-8")

    def _load_pdf(self) -> str:
        """使用 pypdf 解析 PDF，合并所有页文本"""
        from pypdf import PdfReader

        reader = PdfReader(str(self.file_path))
        pages: list[str] = []
        for i, page in enumerate(reader.pages):
            text = page.extract_text()
            if text and text.strip():
                pages.append(f"[第{i+1}页]\n{text.strip()}")
        return "\n\n".join(pages)

    @property
    def file_name(self) -> str:
        return self.file_path.name


def load_documents(directory: str | Path) -> list[dict]:
    """
    批量加载目录下所有支持的文档

    Returns:
        [{"name": "文件名", "content": "文本内容"}, ...]
    """
    dir_path = Path(directory)
    if not dir_path.is_dir():
        raise NotADirectoryError(f"目录不存在: {dir_path}")

    docs: list[dict] = []
    for file_path in sorted(dir_path.iterdir()):
        if file_path.suffix.lower() in DocumentLoader.SUPPORTED_SUFFIXES:
            loader = DocumentLoader(file_path)
            docs.append({"name": loader.file_name, "content": loader.load()})
    return docs
