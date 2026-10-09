// 冒烟：发票匹配台弹窗视觉检查（admin，带 requestId 直接打开上传弹窗）
const { login } = require('./e2e_lib');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PIC = '/home/jvkit/workspace/oa/temp/testing/pic';

(async () => {
  const b = await require('./e2e_lib').launch();
  const { ctx, p } = await login(b, 'admin', 'admin123');
  await p.goto(`${BASE}/procurement/invoice?requestId=2102645300967444481&openUpload=1`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  await p.screenshot({ path: `${PIC}/09-match-dialog.png`, fullPage: false });
  // 看下发票池卡片数量与明细行数
  const cards = await p.locator('.invoice-pool-card, [class*="pool"] [draggable="true"]').count().catch(() => -1);
  const rows = await p.locator('[draggable="true"]').count().catch(() => -1);
  console.log('draggable cards:', rows);
  await b.close();
})().catch(e => { console.error(e.message); process.exit(1); });
