import os
from pathlib import Path

from dotenv import load_dotenv

# 加载 .env（仓库里不放真实 key，部署时从环境/服务器 .env 注入）
# 兼容两种安装方式：
#   1. 源码目录运行（uv run）：config.py 位于 <repo>/src/core/
#   2. pip install -e 安装：src 成为包根，config.py 位于 <repo>/src/core/（同样适用）
_env_candidates = [
    Path(__file__).resolve().parent.parent / ".env",   # <repo>/src/.env
    Path(__file__).resolve().parent.parent.parent / ".env",  # <repo>/.env
]
for _p in _env_candidates:
    if _p.exists():
        load_dotenv(_p)
        break


def _get(name: str, default: str = "") -> str:
    v = os.getenv(name, "").strip()
    return v or default


class Settings:
    # 服务
    host: str = _get("AGENTS_HOST", "127.0.0.1")
    port: int = int(_get("AGENTS_PORT", "8093"))

    # LLM
    llm_base_url: str = _get("LLM_BASE_URL")
    llm_api_key: str = _get("LLM_API_KEY")
    llm_model: str = _get("LLM_MODEL")
    # 视觉模型（发票识别等图片任务），独立端点
    vision_base_url: str = _get("VISION_BASE_URL")
    vision_api_key: str = _get("VISION_API_KEY")
    vision_model: str = _get("VISION_MODEL") or _get("LLM_MODEL")

    # PDF / 窗口
    max_pdf_pages: int = int(_get("MAX_PDF_PAGES", "1"))
    pdf_dpi: int = int(_get("PDF_DPI", "200"))
    max_image_long_edge: int = int(_get("MAX_IMAGE_LONG_EDGE", "1536"))
    concurrency: int = max(1, int(_get("CONCURRENCY", "3")))
    agent_timeout_seconds: int = int(_get("AGENT_TIMEOUT_SECONDS", "60"))

    # Langfuse
    langfuse_public_key: str = _get("LANGFUSE_PUBLIC_KEY")
    langfuse_secret_key: str = _get("LANGFUSE_SECRET_KEY")
    langfuse_host: str = _get("LANGFUSE_HOST")


settings = Settings()
