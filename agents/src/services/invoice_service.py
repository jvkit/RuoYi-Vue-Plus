"""发票识别 + 程序匹配编排。

流程（全并发）：
  对每个 PDF：取第一页转图 → AI 提取字段 → 程序匹配采购申请明细
  最后生成一份人类可读报告（已匹配 / 缺发票 / 外来票）。
"""
import asyncio
import logging

from agents.invoice_agent import InvoiceExtractResult, extract_invoice_from_image
from core.config import settings
from tools import pdf

logger = logging.getLogger(__name__)

# 商品字段（来自采购申请明细）
# 可扩展，只需保持字段名一致即可
MATCH_KEYS = ("name", "spec")


class ItemCandidate:
    """采购申请里的一个商品，来自 RuoYi 转发的明细。"""

    def __init__(self, raw: dict):
        self.id = raw.get("id")
        self.name = str(raw.get("itemName") or raw.get("name") or "").strip()
        self.spec = str(raw.get("spec") or "").strip()
        self.unit_price = _to_float(raw.get("unitPrice") or raw.get("applyPrice"))
        self.quantity = _to_float(raw.get("quantity"))

    def display(self) -> str:
        if self.spec:
            return f"{self.name}({self.spec})"
        return self.name


def _to_float(v) -> float | None:
    try:
        if v is None:
            return None
        return float(str(v).replace(",", "").strip())
    except (TypeError, ValueError):
        return None


def _norm(s: str) -> str:
    """归一化：去空格/全角/大小写，用于名称匹配。"""
    import unicodedata

    s = unicodedata.normalize("NFKC", s)
    return "".join(s.split()).lower()


def _invoice_name(invoice_item) -> str:
    if isinstance(invoice_item, dict):
        return str(invoice_item.get("name") or "")
    return str(getattr(invoice_item, "name", "") or "")


def _match_item(candidate: ItemCandidate, invoice_item) -> bool:
    """纯程序匹配：商品名归一化后包含或相等。"""
    if not candidate.name:
        return False
    inn = _invoice_name(invoice_item)
    cn = _norm(candidate.name)
    inn = _norm(inn)
    if not cn or not inn:
        return False
    if cn == inn:
        return True
    # 双向包含：候选名较长（如带规格）时允许包含关系
    return cn in inn or inn in cn


def _fuzzy_score(candidate: ItemCandidate, invoice_item) -> float:
    """简易相似度 0~1，供匹配打分（保留，后续可加金额约束）。"""
    if not _match_item(candidate, invoice_item):
        return 0.0
    cn = _norm(candidate.name)
    inn = _norm(_invoice_name(invoice_item))
    return min(len(cn), len(inn)) / max(len(cn), len(inn), 1)


async def process_invoice_file(
    filename: str,
    pdf_bytes: bytes,
    candidates: list[ItemCandidate],
    trace_id: str,
) -> dict:
    """处理单个 PDF：返回结构化结果 + 匹配 + 报告段。"""
    page_count = pdf.pdf_page_count(pdf_bytes)
    image = pdf.pdf_first_page_to_image(pdf_bytes)

    result = {
        "originalName": filename,
        "pageCount": page_count,
        "warning": None,
        "extracted": None,
        "matchedItemIds": [],
        "matchedItemNames": [],
        "matchStatus": "no_match",  # no_match / matched / external
        "amountCheck": None,
        "suggestedName": None,
    }

    if image is None:
        result["warning"] = "PDF 无法解析，请检查文件是否损坏"
        return result

    if page_count > settings.max_pdf_pages:
        result["warning"] = f"发票共 {page_count} 页，仅按第一页识别，建议人工核对"

    extracted: InvoiceExtractResult | None = await extract_invoice_from_image(image, filename, trace_id)
    if extracted is None:
        result["warning"] = "AI 识别失败，请稍后重试或人工核对"
        return result

    result["extracted"] = extracted.to_jsonable()

    # ---- 程序匹配（不依赖 AI 判断）----
    # 记录每个匹配到的商品 id → 该发票明细行（单价/数量/金额）
    matched_map: dict[str, dict] = {}  # cand_id -> inv_item
    for inv_item in extracted.items or []:
        for cand in candidates:
            if _match_item(cand, inv_item):
                matched_map.setdefault(str(cand.id), inv_item)
                break

    matched_ids = set(matched_map.keys())
    if matched_ids:
        result["matchedItemIds"] = sorted(matched_ids)
        matched_names = []
        for cid in sorted(matched_ids, key=int) if all(str(x).isdigit() for x in matched_ids) else list(matched_ids):
            c = next((x for x in candidates if str(x.id) == cid), None)
            if c:
                matched_names.append(c.display())
        result["matchedItemNames"] = list(dict.fromkeys(matched_names))
        result["matchStatus"] = "matched"
        base = "_".join(result["matchedItemNames"]) if result["matchedItemNames"] else "发票"
        result["suggestedName"] = f"{base}_发票_{extracted.invoice_number or ''}".rstrip("_")
    else:
        result["matchStatus"] = "external"  # 未匹配到本订单商品，视为外来票/待人工指定

    # ---- 回填单价（不含税口径，供前端自动填"发票单价"）----
    # 每张发票对每个匹配商品：回填该商品在发票明细里的单价 unit_price（验收字段 invoicePrice 是"发票单价"）
    # 若发票明细缺单价，用 amount/quantity；仍无则用票头不含税金额；多商品合并时用第一条匹配的。
    unit_prices: dict[str, float] = {}
    for cid, inv_item in matched_map.items():
        up = None
        if isinstance(inv_item, dict):
            up = inv_item.get("unit_price")
            if up is None and inv_item.get("amount") is not None and inv_item.get("quantity"):
                try:
                    up = float(inv_item["amount"]) / float(inv_item["quantity"])
                except (TypeError, ValueError, ZeroDivisionError):
                    up = None
        else:
            up = getattr(inv_item, "unit_price", None)
        if isinstance(up, (int, float)):
            unit_prices[cid] = round(float(up), 2)
    # 没有明细单价时，fallback 到票头不含税金额（仅单商品时合理）
    if not unit_prices and extracted.amount_without_tax is not None and len(matched_ids) == 1:
        unit_prices[next(iter(matched_ids))] = round(float(extracted.amount_without_tax), 2)
    result["matchedUnitPrices"] = unit_prices
    # 兼容旧字段：单商品回填其单价；多商品给平均/第一条（前端只用于展示提示）
    result["invoiceAmount"] = (unit_prices[next(iter(unit_prices))] if unit_prices else None)

    # 程序兜底：AI 有时漏判 is_red_invoice，但单价为负已说明是冲红，强制修正
    if not extracted.is_red_invoice and unit_prices and any(v < 0 for v in unit_prices.values()):
        extracted.is_red_invoice = True
        result["extracted"]["is_red_invoice"] = True

    # ---- 金额核对（程序，不含税口径，逐商品比对单价）----
    if matched_ids:
        checks = []
        for cid in sorted(unit_prices.keys()):
            cand = next((c for c in candidates if str(c.id) == cid), None)
            if not cand or not cand.unit_price:
                continue
            actual = unit_prices[cid]
            expected = float(cand.unit_price)
            if actual is None:
                continue
            if extracted.is_red_invoice:
                status = "red_invoice"
            elif abs(actual) > expected * 1.05:
                status = "amount_exceed"
            elif abs(actual) < expected * 0.95:
                status = "amount_lower"
            else:
                status = "ok"
            checks.append({"itemId": cid, "expected": round(expected, 2), "actual": round(actual, 2), "status": status})
        if checks:
            result["amountCheck"] = {
                "items": checks,
                "summary": next((c for c in checks if c["status"] != "ok"), checks[0])["status"],
            }

    return result


async def process_invoice_batch(
    files: list[tuple[str, bytes]],
    items_raw: list[dict],
    trace_id: str,
) -> dict:
    """并发处理一批发票 PDF，汇总报告。"""
    candidates = [ItemCandidate(r) for r in items_raw]

    sem = asyncio.Semaphore(settings.concurrency)

    async def bounded(fn, *args):
        async with sem:
            return await fn(*args)

    results = await asyncio.gather(
        *[bounded(process_invoice_file, name, data, candidates, trace_id) for name, data in files]
    )

    # ---- 汇总报告 ----
    matched = [r for r in results if r["matchStatus"] == "matched"]
    no_match_invoices = [r for r in results if r["matchStatus"] == "external"]

    # 已匹配的商品 id（去重）
    matched_item_ids = set()
    for r in matched:
        matched_item_ids.update(r["matchedItemIds"])
    unmatched_items = [c for c in candidates if str(c.id) not in matched_item_ids]

    report_lines = []
    red_invoice_count = 0
    if matched:
        for r in matched:
            names = r.get("matchedItemNames")
            display = "、".join(names) if names else r["originalName"]
            is_red = bool(r.get("extracted") and r["extracted"].get("is_red_invoice"))
            if is_red:
                red_invoice_count += 1
                report_lines.append(f"🔴 发票「{r['originalName']}」为冲红发票，已匹配商品「{display}」")
            else:
                report_lines.append(f"✅ 发票「{r['originalName']}」→ 商品「{display}」")
    if unmatched_items:
        for c in unmatched_items:
            report_lines.append(f"⚠️ 商品「{c.display()}」暂无对应发票，可继续补充上传")
    for r in no_match_invoices:
        report_lines.append(f"❌ 发票「{r['originalName']}」不属于本订单，未匹配到商品")

    summary = {
        "matchedInvoiceCount": len(matched),
        "matchedItemCount": len(matched_item_ids),
        "unmatchedItemCount": len(unmatched_items),
        "externalInvoiceCount": len(no_match_invoices),
        "redInvoiceCount": red_invoice_count,
        "lines": report_lines,
    }

    return {
        "traceId": trace_id,
        "results": results,
        "summary": summary,
    }
