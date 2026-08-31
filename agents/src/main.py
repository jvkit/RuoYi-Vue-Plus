import logging

import uvicorn

from api.routes import router as invoice_router
from core.config import settings

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

from fastapi import FastAPI  # noqa: E402

app = FastAPI(title="OA Agents", version="0.1.0")
app.include_router(invoice_router)


@app.get("/health")
async def health():
    return {"ok": True}


if __name__ == "__main__":
    uvicorn.run(app, host=settings.host, port=settings.port)
