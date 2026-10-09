// 冒烟：工作台点「上传发票」原地弹窗，不跳转
const { launch, login } = require('./e2e_lib');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PIC = '/home/jvkit/workspace/oa/temp/testing/pic';

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'admin', 'admin123');
  await p.goto(`${BASE}/procurement/invoiceWorkbench`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2000);
  const urlBefore = p.url();
  await p.locator('button:has-text("上传发票")').first().click();
  await p.waitForTimeout(2500);
  const urlAfter = p.url();
  const dialogVisible = await p.locator('.el-dialog__title', { hasText: '发票匹配台' }).count();
  console.log('url same:', urlBefore === urlAfter, '| dialog:', dialogVisible > 0 ? 'VISIBLE' : 'MISSING');
  await p.screenshot({ path: `${PIC}/11-workbench-dialog.png` });
  await b.close();
})().catch(e => { console.error('ERR', e.message); process.exit(1); });
