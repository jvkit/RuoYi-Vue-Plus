// E2E 阶段4：发票台账 AI 一键匹配（9 张本单发票 + 3 张噪声发票）
const fs = require('fs');
const { launch, login, selectOptionByLabel, shot, fail } = require('./e2e_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const INV = '/home/jvkit/workspace/oa/temp/testing/采购素材(1)/采购素材';
const PDFS = [
  // 本单 5 张（应匹配）
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/5-螺丝-448.52元.pdf`,
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/6-螺母-241.45.pdf`,
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/1-加热带-70.00元.pdf`,
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/7-内存卡发票-415.pdf`,
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/9.GX12公头转XH2.54端子-39.9.pdf`,
  // 本单外 4 张（不应匹配：明细不存在）
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/2-typec双公头定制-120.00元.pdf`,
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/3-XH2.54_4P-5.90元.pdf`,
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/4-USB2.0双公头数据线-9.40元.pdf`,
  `${INV}/8.0105淘宝件验收图/8.0105采购发票/8.Type-C母座发票-6.6.pdf`,
  // 完全无关 3 张（噪声）
  `${INV}/11.工具+激光器采购验收图/发票/未知发票1.pdf`,
  `${INV}/11.工具+激光器采购验收图/发票/151615616651.pdf`,
  `${INV}/11.工具+激光器采购验收图/发票/489461565.pdf`,
];

(async () => {
  for (const f of PDFS) if (!fs.existsSync(f)) console.error('缺文件:', f);
  const b = await launch();
  const { p } = await login(b, 'admin', 'admin123');
  try {
    await p.goto(`${BASE}/procurement/invoice`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    await shot(p, '04-1-invoice-ledger-empty');

    await p.locator('button:has-text("上传发票")').first().click();
    await p.waitForTimeout(1500);
    const dlg = p.locator('.el-dialog:visible', { hasText: '上传发票' }).first();
    // 关联申请
    await selectOptionByLabel(p, dlg, '关联申请', '0105');
    await p.waitForTimeout(1000);
    // 上传 12 张 PDF
    await dlg.getByRole('tabpanel', { name: 'AI 一键匹配' }).locator('input[name=file]').setInputFiles(PDFS);
    await p.waitForTimeout(2000);
    await shot(p, '04-2-invoice-files-ready');

    // 开始识别
    await dlg.locator('button:has-text("开始识别")').click();
    console.log('AI 识别中（12 张，可能需 1-3 分钟）...');
    const btn = dlg.locator('button:has-text("开始识别")');
    // 等 loading 出现
    for (let i = 0; i < 12; i++) { await p.waitForTimeout(1000); const c = await btn.getAttribute('class'); if (c && c.includes('is-loading')) break; }
    // 等 loading 消失（最多 6 分钟）
    for (let i = 0; i < 72; i++) {
      await p.waitForTimeout(5000);
      const c = await btn.getAttribute('class').catch(() => '');
      if (!c || !c.includes('is-loading')) break;
    }
    // 前端 axios 50s 超时会先报错，但后端(300s)与 agents 继续处理
    await p.waitForTimeout(60000);
    await shot(p, '04-3-invoice-ai-timeout-expected');
    console.log('前端已超时(预期)，继续等后端落库...');
    await p.waitForTimeout(180000);
    await shot(p, '04-4-invoice-after-wait');
    // 关闭弹窗刷新台账
    const closeBtn = p.locator('.el-dialog:visible button:has-text("关闭")').first();
    if (await closeBtn.count()) { await closeBtn.click(); await p.waitForTimeout(1000); }
    await p.reload({ waitUntil: 'networkidle' });
    await p.waitForTimeout(3000);
    await shot(p, '04-5-invoice-ledger-final');
    console.log('识别完成');
  } catch (e) {
    await fail(p, e, 'stage4');
  } finally {
    await b.close();
  }
})();
