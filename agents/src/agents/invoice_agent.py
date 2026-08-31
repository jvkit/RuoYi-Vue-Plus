"""发票识别 Agent：只做“看图 → 结构化字段”，不做任何业务判断。

所有发票常见字段都尝试提取，识别不到返回 None。
是否冲红、金额是否超标、属于哪个商品 —— 这些全是程序的活，AI 不负责。
"""
from pydantic import BaseModel, Field
from pydantic_ai import Agent
from pydantic_ai.messages import BinaryContent
from pydantic_ai.models.openai import OpenAIChatModel
from pydantic_ai.providers.openai import OpenAIProvider

from core.config import settings
from core.langfuse import trace, get_prompt


class InvoiceItem(BaseModel):
    name: str | None = Field(None, description="商品/服务名称")
    spec: str | None = Field(None, description="规格型号")
    unit: str | None = Field(None, description="单位")
    quantity: float | None = Field(None, description="数量")
    unit_price: float | None = Field(None, description="单价")
    amount: float | None = Field(None, description="金额")
    tax_rate: str | None = Field(None, description="税率/征收率")


class InvoiceExtractResult(BaseModel):
    invoice_code: str | None = Field(None, description="发票代码（数电票通常无）")
    invoice_number: str | None = Field(None, description="发票号码")
    invoice_type: str | None = Field(None, description="发票类型，如 增值税专用发票/增值税普通发票/电子发票")
    invoice_date: str | None = Field(None, description="开票日期 yyyy-MM-dd")
    seller_name: str | None = Field(None, description="销售方名称")
    seller_tax_no: str | None = Field(None, description="销售方税号")
    buyer_name: str | None = Field(None, description="购买方名称")
    buyer_tax_no: str | None = Field(None, description="购买方税号")
    total_amount: float | None = Field(None, description="价税合计")
    tax_amount: float | None = Field(None, description="税额")
    amount_without_tax: float | None = Field(None, description="不含税金额")
    is_red_invoice: bool = Field(False, description="是否红字/冲红发票（金额为负数或注明红字）")
    items: list[InvoiceItem] = Field(default_factory=list, description="商品/服务明细")

    def to_jsonable(self) -> dict:
        return self.model_dump(mode="json")


_SYSTEM_PROMPT = """你是一名专业的发票识别助手。请仔细查看发票图片，提取票面上所有常见字段。

规则：
1. 只提取、不做任何判断、不下任何结论（如是否合规、是否超标、是否属于某订单）。
2. 识别不到或图上没有的字段，一律返回 null，不要编造。
3. 金额字段只填数字（如 1234.56），不要带货币符号和千分位逗号；负数金额保留负号。
   - amount_without_tax 只填「不含税金额」（即发票票面的"金额/未税金额"栏），不要填价税合计。
   - total_amount 只填「价税合计」（票面最下方的总计），没有就填 null。
   - tax_amount 只填「税额」，没有就填 null。
4. 开票日期填 yyyy-MM-dd。
5. 商品/服务明细逐行列出：名称、规格型号、单位、数量、单价、金额、税率。
   其中"金额"为该行不含税金额，单价*数量=金额；若票面只给含税金额，按税率先除税再填，或填 null。
6. 如果发票是红字发票（金额为负数，或票面注明"红字/负数/冲红"），is_red_invoice 设为 true。
7. 字段名固定用英文 key（如 seller_name、amount_without_tax、total_amount、is_red_invoice），不要用中文 key。
"""

# langfuse 中的 Prompt 名（网页可改，改完按版本发布即可，无需改代码）
PROMPT_NAME = "invoice_extract"


def build_invoice_agent() -> Agent:
    provider = OpenAIProvider(
        base_url=settings.vision_base_url or None,
        api_key=settings.vision_api_key or None,
    )
    model = OpenAIChatModel(settings.vision_model, provider=provider)
    # 提示词优先从 langfuse 按 PROMPT_NAME 加载，未配置/失败回退源码默认
    system_prompt = get_prompt(PROMPT_NAME, fallback=_SYSTEM_PROMPT)
    return Agent(
        model=model,
        output_type=InvoiceExtractResult,
        system_prompt=system_prompt,
        model_settings={"max_tokens": 2048, "timeout": settings.agent_timeout_seconds},
    )


async def extract_invoice_from_image(image_bytes: bytes, filename: str, trace_id: str, retries: int = 1) -> InvoiceExtractResult | None:
    """对单张发票图片做结构化提取。返回 None 表示 AI 多次调用后仍失败/无法解析。

    retries：偶发失败时的重试次数（视觉模型偶发超时/空返回，重试一次可大幅降低误判"无效票"）。
    """
    import logging

    log = logging.getLogger(__name__)
    agent = build_invoice_agent()
    prompt = get_prompt(PROMPT_NAME, fallback=_SYSTEM_PROMPT)
    span_input = {"filename": filename, "prompt_name": PROMPT_NAME, "model": settings.vision_model, "image_bytes": len(image_bytes)}
    last_err: Exception | None = None
    for attempt in range(retries + 1):
        try:
            async with trace("invoice.extract", trace_id=trace_id) as span:
                span.update(input=span_input)
                try:
                    result = await agent.run(
                        [
                            "请识别这张发票，按 schema 输出结构化字段。",
                            BinaryContent(data=image_bytes, media_type="image/jpeg"),
                        ]
                    )
                except Exception as e:  # noqa: BLE001
                    span.update(output={"error": str(e)})
                    raise
                span.update(output=result.output.to_jsonable() if result.output else None)
                return result.output
        except Exception as e:  # noqa: BLE001
            last_err = e
            log.warning("extract_invoice failed (%s), attempt %d/%d: %s", filename, attempt + 1, retries + 1, e)
    log.error("extract_invoice gave up (%s): %s", filename, last_err)
    return None
