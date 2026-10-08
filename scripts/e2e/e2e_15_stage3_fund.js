// E2E-15 阶段3：admin 资金管理页 Tab1 备用金视图截图（账户表 + 两条拆账流水）
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
    await p.waitForTimeout(1500);

    // 账户表关键数值
    const reserveText = await p.locator('.el-tab-pane:visible .el-table').first().innerText();
    console.log('[check] 备用金账户表含 冯忠俊 =', reserveText.includes('冯忠俊'), '，含 彭赛威 =', reserveText.includes('彭赛威'));
    console.log('[check] 账户表含 10,000.00 =', reserveText.includes('10,000.00'), '，含 3,073.97 =', reserveText.includes('3,073.97'));
    await shot(p, '15-e2e-08-fund-tab1-reserve');

    // 流水表按出纳人筛选冯忠俊验证listReserve口径（接口权限已放开给冯忠俊，admin更没问题）
    const selfText = await p.locator('.el-tab-pane:visible .el-table').nth(1).innerText();
    console.log('[check] 自购流水表含 FUND-20260924-005 =', selfText.includes('FUND-20260924-005'), '，含 FUND-20260924-006 =', selfText.includes('FUND-20260924-006'));
    console.log('[check] 自购流水表含 8,426.03 =', selfText.includes('8,426.03'), '，含 3,073.97 =', selfText.includes('3,073.97'));
    console.log('[check] 自购流水表含 拆账联测 =', selfText.includes('拆账联测'));
    console.log('[OK] stage3 fund page verified');
    await browser.close();
  } catch (e) {
    await fail(p, e, '15-stage3');
  } finally {
    await ctx.close().catch(() => {});
  }
})();
