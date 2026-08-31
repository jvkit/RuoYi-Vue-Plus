"""Langfuse 追踪 + Prompt 管理（适配 langfuse 4.x OTel-native API）。

所有 AI 调用都走这里，保证可观测：
- tracing：每次 AI 调用记录输入/输出/耗时，出错时能区分"程序问题"还是"AI 问题"
- prompt：提示词从 Langfuse 按 name+version 加载，改提示词无需改代码；未配置时回退源码默认值
"""
import contextlib
import logging
import uuid
from typing import AsyncIterator

from langfuse import Langfuse
from langfuse.types import TraceContext

from core.config import settings

logger = logging.getLogger(__name__)

# Langfuse 客户端（key 未配置时不初始化，保持可用）
langfuse: Langfuse | None = None
if settings.langfuse_public_key and settings.langfuse_secret_key and settings.langfuse_host:
    try:
        langfuse = Langfuse(
            public_key=settings.langfuse_public_key,
            secret_key=settings.langfuse_secret_key,
            host=settings.langfuse_host,
        )
        logger.info("Langfuse tracing enabled at %s", settings.langfuse_host)
    except Exception as e:  # noqa: BLE001
        logger.warning("Langfuse init failed, tracing disabled: %s", e)
        langfuse = None


def new_trace_id() -> str:
    # langfuse 4.x 要求 trace_id 为 32 位小写 hex（等同无横线 UUID）
    return uuid.uuid4().hex


@contextlib.asynccontextmanager
async def trace(name: str, trace_id: str | None = None, as_type: str = "agent") -> AsyncIterator:
    """异步追踪上下文：进入记录 input，退出记录 output/error。

    未配置 langfuse 时退化为 no-op，不抛错。
    用法：
        async with trace("invoice.extract", trace_id=rid) as span:
            span.update(input={...})
            result = await agent.run(...)
            span.update(output=result)
    """
    if langfuse is None:
        yield _NoopSpan()
        return
    tc = TraceContext(trace_id=trace_id or new_trace_id())
    with langfuse.start_as_current_observation(name=name, trace_context=tc, as_type=as_type) as span:  # type: ignore[arg-type]
        yield _SpanProxy(span)


class _NoopSpan:
    def update(self, **kwargs):
        return self


class _SpanProxy:
    def __init__(self, span):
        self._span = span

    def update(self, **kwargs):
        try:
            self._span.update(**kwargs)
        except Exception as e:  # noqa: BLE001
            logger.debug("langfuse span update failed: %s", e)
        return self


def get_prompt(name: str, fallback: str, *, version: int | None = None, **kwargs) -> str:
    """从 Langfuse 按 name+version 加载提示词；未配置/失败时回退到源码默认值。"""
    if langfuse is not None:
        try:
            p = langfuse.get_prompt(name, version=version)
            compiled = p.compile(**kwargs)
            if compiled:
                return compiled
        except Exception as e:  # noqa: BLE001
            logger.warning("Langfuse prompt %s load failed, using fallback: %s", name, e)
    try:
        return fallback.format(**kwargs)
    except (KeyError, IndexError):
        return fallback
