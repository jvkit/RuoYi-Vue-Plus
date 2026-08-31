"""FastAPI 路由：发票识别与匹配。"""
import logging

from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from core.langfuse import new_trace_id
from services.invoice_service import process_invoice_batch

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/invoice", tags=["invoice"])


@router.post("/match")
async def match_invoices(
    items: str = Form(..., description="采购申请明细 JSON 数组，如 [{id,name,spec,unitPrice,quantity}]"),
    files: list[UploadFile] = File(..., description="发票 PDF 文件（可多个）"),
):
    """批量上传发票 PDF，AI 提取字段 + 程序匹配采购申请明细，返回匹配报告。"""
    trace_id = new_trace_id()

    try:
        import json

        items_raw = json.loads(items)
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=400, detail=f"items 参数不是合法 JSON：{e}") from e

    pdf_files: list[tuple[str, bytes]] = []
    for f in files:
        data = await f.read()
        if not data:
            continue
        pdf_files.append((f.filename or "未命名.pdf", data))

    if not pdf_files:
        raise HTTPException(status_code=400, detail="未收到 PDF 文件")

    try:
        result = await process_invoice_batch(pdf_files, items_raw, trace_id)
    except Exception as e:  # noqa: BLE001
        logger.exception("invoice match failed")
        raise HTTPException(status_code=500, detail=f"AI 服务处理失败：{e}") from e

    return {"code": 200, "data": result}
