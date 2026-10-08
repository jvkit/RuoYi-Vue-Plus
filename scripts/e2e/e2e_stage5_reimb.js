// E2E 阶段5：报销导出（生成报销包 + 下载 ZIP 到 testing/output）
const path = require('path');
const fs = require('fs');
const { launch, login, selectOptionByLabel, shot, fail } = require('./e2e_lib');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const OUT = '/home/jvkit/workspace/oa/temp/testing/output';

(async () => {
  const b = await launch();
  const { p } = await login(b, 'admin', 'admin123');
  try {
    await p.goto(`${BASE}/procurement/reimbursement`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    await shot(p, '05-1-reimb-list');

    // 生成报销包
    await p.locator('button:has-text("生成报销包")').first().click();
    await p.waitForTimeout(1500);
    const dlg = p.locator('.el-dialog:visible', { hasText: '生成报销包' }).first();
    await selectOptionByLabel(p, dlg, '关联申请', '0105');
    await p.waitForTimeout(1200);
    await shot(p, '05-2-pack-dialog');
    await dlg.locator('button:has-text("打 包")').click();
    await p.waitForTimeout(4000);
    await shot(p, '05-3-packed');

    // 选中行并下载
    await p.reload({ waitUntil: 'networkidle' });
    await p.waitForTimeout(3000);
    const row = p.locator('.el-table__row').first();
    await row.locator('.el-checkbox').first().click();
    await p.waitForTimeout(800);
    const [download] = await Promise.all([
      p.waitForEvent('download', { timeout: 60000 }),
      p.locator('button:has-text("下载")').first().click()
    ]);
    const fname = download.suggestedFilename();
    const target = path.join(OUT, fname);
    await download.saveAs(target);
    console.log('下载完成:', target, fs.statSync(target).size, 'bytes');
    await shot(p, '05-4-downloaded');
  } catch (e) {
    await fail(p, e, 'stage5');
  } finally {
    await b.close();
  }
})();
