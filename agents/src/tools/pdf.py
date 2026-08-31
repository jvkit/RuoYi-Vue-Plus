"""确定性 PDF 处理：转图、压缩。AI 只负责看图，不负责解析 PDF。"""
import io

import fitz  # PyMuPDF
from PIL import Image

from core.config import settings


def pdf_first_page_to_image(pdf_bytes: bytes) -> bytes | None:
    """取 PDF 第一页转 JPEG（可选长边压缩）。失败返回 None。"""
    try:
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        if doc.page_count == 0:
            return None
        page = doc[0]
        pix = page.get_pixmap(dpi=settings.pdf_dpi)
        img = Image.frombytes("RGB", [pix.width, pix.height], pix.samples)
        img = _resize_long_edge(img, settings.max_image_long_edge)
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=85)
        return buf.getvalue()
    except Exception:
        return None


def pdf_page_count(pdf_bytes: bytes) -> int:
    try:
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        return doc.page_count
    except Exception:
        return 0


def _resize_long_edge(img: Image.Image, max_edge: int) -> Image.Image:
    w, h = img.size
    longest = max(w, h)
    if longest <= max_edge:
        return img
    ratio = max_edge / longest
    return img.resize((max(1, int(w * ratio)), max(1, int(h * ratio))), Image.LANCZOS)
