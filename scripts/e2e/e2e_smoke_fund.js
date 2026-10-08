// 资金管理页重写冒烟（docs/9.20/设计探讨-备用金与报销流程.md §2 整卡大切换版）：
// Tab1 备用金视图（账户+自购流水）截图 → Tab1 登记支出弹窗截图 → Tab2 项目视图截图 →
// Tab3 资金状态看板截图 → Tab2 登记一条对公人工支出 88.88（备注「页面重写冒烟-对公直支88.88」）→ 验证流水出现（数据保留）
const { launch, login, fillByLabel, openSelectByLabel, shot, fail, PIC } = require('./e2e_lib');
const fs = require('fs');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
fs.mkdirSync(PIC, { recursive: true });

/** 展开项目树下拉全部节点后点第一个叶子节点 */
async function pickFirstLeafProject(page, scope, label) {
  await openSelectByLabel(page, scope, label);
  const pop = page.locator('.el-tree-select__popper:visible').last();
  for (let round = 0; round < 10; round++) {
    const collapsed = pop.locator('.el-tree-node__expand-icon:not(.is-leaf):not(.expanded)');
    if ((await collapsed.count()) === 0) break;
    await collapsed.first().click();
    await page.waitForTimeout(400);
  }
  // 叶子 = 自身 content 直接子级的展开图标带 is-leaf（:has() 不能写成后代匹配，否则父节点因嵌套叶子被误判）
  const leafSel = '.el-tree-node:has(> .el-tree-node__content > .el-tree-node__expand-icon.is-leaf)';
  const leaf = pop.locator(`${leafSel} > .el-tree-node__content .el-select-dropdown__item span`).first();
  const leafText = (await leaf.innerText()).trim();
  await leaf.click();
  await page.waitForTimeout(400);
  await page.keyboard.press('Escape');
  await page.waitForTimeout(300);
  return leafText;
}

(async () => {
  const browser = await launch();
  const { ctx, p } = await login(browser, 'admin', 'admin123');
  try {
    await p.goto(`${BASE}/procurement/fund`, { waitUntil: 'networkidle' });
    // Tab1 备用金视图（默认）：账户表 + 自购流水表
    await p.waitForSelector('.el-tabs__item', { timeout: 15000 });
    await p.waitForSelector('.el-table__row', { timeout: 15000 });
    await p.waitForTimeout(1000);
    await shot(p, '13-fund-tab1');

    // Tab1 登记支出弹窗（自购：含 ReservePicker）
    await p.locator('.el-tab-pane:visible button:has-text("登记支出")').first().click();
    let dialog = p.locator('.el-dialog:visible').last();
    await dialog.waitFor({ timeout: 10000 });
    await p.waitForTimeout(800);
    const rpVisible = await dialog.locator('.reserve-picker').isVisible().catch(() => false);
    console.log('[check] 自购弹窗 reserve-picker 可见 =', rpVisible);
    if (!rpVisible) throw new Error('自购登记弹窗未展示 ReservePicker');
    const rpCards = await dialog.locator('.rp-card').count();
    console.log('[check] 自购弹窗备用金卡片数 =', rpCards);
    await shot(p, '13-fund-add');
    await dialog.locator('button:has-text("取 消")').click();
    await p.waitForTimeout(600);

    // Tab2 项目视图：项目树汇总 + 对公流水
    await p.locator('.el-tabs__item', { hasText: '项目视图' }).first().click();
    await p.waitForTimeout(1200);
    await shot(p, '13-fund-tab2');

    // Tab3 资金状态看板：两张统计卡 + 状态分布
    await p.locator('.el-tabs__item', { hasText: '资金状态看板' }).first().click();
    await p.waitForTimeout(1200);
    const boardCards = await p.locator('.board-card').count();
    console.log('[check] 看板状态卡数 =', boardCards);
    await shot(p, '13-fund-tab3');

    // 回 Tab2 登记一条对公人工支出 88.88
    await p.locator('.el-tabs__item', { hasText: '项目视图' }).first().click();
    await p.waitForTimeout(800);
    await p.locator('.el-tab-pane:visible button:has-text("登记支出")').first().click();
    dialog = p.locator('.el-dialog:visible').last();
    await dialog.waitFor({ timeout: 10000 });
    await p.waitForTimeout(600);
    const rpVisible2 = await dialog.locator('.reserve-picker').isVisible().catch(() => false);
    console.log('[check] 对公弹窗 reserve-picker 可见 =', rpVisible2);
    if (rpVisible2) throw new Error('对公登记弹窗不应展示 ReservePicker');

    const leafName = await pickFirstLeafProject(p, dialog, '项目');
    console.log('[check] 选中叶子项目 =', leafName);
    await fillByLabel(p, dialog, '金额', '88.88');
    await dialog.locator('.el-form-item', { hasText: '备注' }).locator('textarea').first().fill('页面重写冒烟-对公直支88.88');
    await p.waitForTimeout(300);
    await dialog.locator('button:has-text("确 定")').click();
    await p.waitForTimeout(1500);

    // 验证流水出现（对公流水表翻页查找备注关键字；数据保留不清理）
    let found = false;
    for (let pageNo = 0; pageNo < 5 && !found; pageNo++) {
      const tableText = await p.locator('.el-tab-pane:visible .el-table').last().innerText();
      found = tableText.includes('88.88') && tableText.includes('页面重写冒烟');
      if (found) break;
      const nextBtn = p.locator('.el-tab-pane:visible .el-pagination button.btn-next:not([disabled])').first();
      if (!(await nextBtn.isVisible().catch(() => false))) break;
      await nextBtn.click();
      await p.waitForTimeout(800);
    }
    console.log('[check] 流水表找到 88.88 + 备注 =', found);
    if (!found) throw new Error('对公人工流水未出现在列表');
    await shot(p, '13-fund-after-add');

    console.log('[OK] fund page smoke passed');
    await browser.close();
  } catch (e) {
    await fail(p, e, 'fund');
  } finally {
    await ctx.close().catch(() => {});
  }
})();
