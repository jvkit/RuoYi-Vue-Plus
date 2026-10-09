// E2E-15 前置：admin 在资金管理页给彭赛威新建备用金账户（幂等：已存在则跳过）
const { launch, login, shot, fail, PIC } = require('./e2e_lib');
const fs = require('fs');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
fs.mkdirSync(PIC, { recursive: true });

(async () => {
  const browser = await launch();
  const { ctx, p } = await login(browser, 'admin', 'admin123');
  try {
    await p.goto(`${BASE}/procurement/fund`, { waitUntil: 'networkidle' });
    await p.waitForSelector('.el-tabs__item', { timeout: 15000 });
    await p.waitForSelector('.el-table__row', { timeout: 15000 });
    await p.waitForTimeout(1000);

    const existing = await p.locator('.el-tab-pane:visible .el-table__row', { hasText: '彭赛威' }).count();
    console.log('[setup] 彭赛威账户已存在 =', existing > 0);
    if (existing > 0) {
      await shot(p, '15-e2e-00-reserve-skip-exists');
    } else {
      await p.locator('.el-tab-pane:visible button:has-text("新增账户")').first().click();
      const dialog = p.locator('.el-dialog:visible').last();
      await dialog.waitFor({ timeout: 10000 });
      await p.waitForTimeout(600);
      // 人员下拉选彭赛威
      await dialog.locator('.el-form-item', { hasText: '人员' }).locator('.el-select__wrapper').first().click();
      await p.waitForTimeout(700);
      await p.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '彭赛威' }).first().click();
      await p.waitForTimeout(400);
      // 额度默认 10000，不动
      await shot(p, '15-e2e-00-reserve-add-dialog');
      await dialog.locator('button:has-text("确 定")').click();
      await p.waitForTimeout(1500);
      const ok = await p.locator('.el-tab-pane:visible .el-table__row', { hasText: '彭赛威' }).count();
      console.log('[setup] 彭赛威账户创建后出现 =', ok > 0);
      if (!ok) throw new Error('彭赛威备用金账户创建后未出现在列表');
      await shot(p, '15-e2e-00-reserve-added');
    }
    console.log('[OK] setup reserve account done');
    await browser.close();
  } catch (e) {
    await fail(p, e, '15-setup');
  } finally {
    await ctx.close().catch(() => {});
  }
})();
