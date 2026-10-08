// 备用金选人组件（ReservePicker）冒烟：
// 新增采购申请(自购) → 平铺卡片展示 → 加明细产生金额 → 点 1 人触发"1 号够用锁定"置灰 →
// 取消后点 2 人看序号与重排 → 使用人下拉 → 切对公看组件消失
const { launch, login, fillByLabel, treeSelectByLabel, shot, fail, PIC } = require('./e2e_lib');
const fs = require('fs');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
fs.mkdirSync(PIC, { recursive: true });

(async () => {
  const browser = await launch();
  const { ctx, p } = await login(browser, 'admin', 'admin123');
  try {
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForSelector('.el-table__row', { timeout: 15000 });
    await p.waitForTimeout(800);

    // 打开新增弹窗（默认采购方式=自购）
    await p.locator('button:has-text("新增")').first().click();
    const dialog = p.locator('.el-dialog:visible').last();
    await dialog.waitFor({ timeout: 10000 });
    await p.waitForTimeout(600);

    // 备用金人平铺卡片 + 使用人下拉（未选状态）
    const cards = dialog.locator('.rp-card');
    const cardCount = await cards.count();
    console.log('[check] rp-card count =', cardCount);
    if (cardCount < 2) throw new Error('备用金卡片不足 2 张，无法验证多选');

    // 使用人下拉可打开且有选项
    await dialog.locator('.el-form-item', { hasText: '使用人' }).locator('.el-select__wrapper').first().click();
    await p.waitForTimeout(700);
    const userOpts = await p.locator('.el-select-dropdown:visible .el-select-dropdown__item').count();
    console.log('[check] 使用人 options =', userOpts);
    await shot(p, '12-reserve-picker-users');
    await p.keyboard.press('Escape');
    await p.waitForTimeout(300);

    // 加一条明细（超大金额 99999999），1 号不够用 → 可多选验证序号与重排
    await dialog.locator('button:has-text("添加明细")').first().click();
    const itemDialog = p.locator('.el-dialog:visible').last();
    await itemDialog.waitFor({ timeout: 8000 });
    await fillByLabel(p, itemDialog, '品名', '冒烟测试物品');
    await fillByLabel(p, itemDialog, '数量', '1');
    await fillByLabel(p, itemDialog, '单价', '99999999');
    await itemDialog.locator('button:has-text("确 定")').click();
    await p.waitForTimeout(500);

    // 点两人：序号 1、2（1 号不够用，其余卡不置灰）
    await cards.nth(0).click();
    await p.waitForTimeout(300);
    await cards.nth(1).click();
    await p.waitForTimeout(400);
    const orders = await cards.evaluateAll((els) =>
      els.map((e) => {
        const badge = e.querySelector('.rp-order');
        return badge ? badge.textContent : '';
      })
    );
    const disabledAfter2 = await cards.evaluateAll((els) => els.map((e) => e.classList.contains('is-disabled')));
    const tip2 = await dialog.locator('.rp-tip').innerText();
    console.log('[check] 两人序号 =', JSON.stringify(orders));
    console.log('[check] 选 2 人后各卡 disabled =', JSON.stringify(disabledAfter2));
    console.log('[check] tip =', tip2);
    if (orders[0] !== '1' || orders[1] !== '2') throw new Error('两人序号不对: ' + JSON.stringify(orders));
    await shot(p, '12-reserve-picker-2person');

    // 取消第 1 人，看后续序号重排（原 2 号变 1 号）
    await cards.nth(0).click();
    await p.waitForTimeout(400);
    const ordersAfterReorder = await cards.evaluateAll((els) =>
      els.map((e) => {
        const badge = e.querySelector('.rp-order');
        return badge ? badge.textContent : '';
      })
    );
    console.log('[check] 重排后序号 =', JSON.stringify(ordersAfterReorder));
    if (ordersAfterReorder[1] !== '1') throw new Error('取消 1 号后序号未重排: ' + JSON.stringify(ordersAfterReorder));

    // 把明细单价改回 100 → 1 号可用(≈10000) >= 100 → 触发"1 号够用锁定"
    await dialog.locator('.el-table .el-button--primary').first().click();
    const itemDialog2 = p.locator('.el-dialog:visible').last();
    await itemDialog2.waitFor({ timeout: 8000 });
    await fillByLabel(p, itemDialog2, '单价', '100');
    await itemDialog2.locator('button:has-text("确 定")').click();
    await p.waitForTimeout(500);

    // 全取消后重选第 1 人：其余卡片置灰 + 提示"1 号够用，已锁定"
    await cards.nth(1).click();
    await p.waitForTimeout(300);
    await cards.nth(0).click();
    await p.waitForTimeout(400);
    const disabledAfter1 = await cards.evaluateAll((els) => els.map((e) => e.classList.contains('is-disabled')));
    const tip1 = await dialog.locator('.rp-tip').innerText();
    console.log('[check] 选 1 人后各卡 disabled =', JSON.stringify(disabledAfter1));
    console.log('[check] tip =', tip1);
    if (!disabledAfter1[1] || !disabledAfter1[2]) throw new Error('1 号够用时其余卡片未置灰');
    await shot(p, '12-reserve-picker-locked');

    // 切对公：ReservePicker 与使用人表单项消失
    await dialog.locator('.el-radio:visible', { hasText: '对公' }).first().click();
    await p.waitForTimeout(500);
    const rpVisible = await dialog.locator('.reserve-picker').isVisible().catch(() => false);
    const useUserVisible = await dialog.locator('.el-form-item', { hasText: '使用人' }).isVisible().catch(() => false);
    console.log('[check] 对公下 reserve-picker 可见 =', rpVisible, '，使用人可见 =', useUserVisible);
    if (rpVisible || useUserVisible) throw new Error('切对公后备用金人/使用人仍可见');
    await shot(p, '12-reserve-picker-gong');

    /* ------- 回显闭环：切回自购 → 存草稿 → 修改回显 → 详情展示 ------- */
    // 金额改回超大，避免"1 号够用锁定"挡住第二人
    await dialog.locator('.el-table .el-button--primary').first().click();
    const itemDialog3 = p.locator('.el-dialog:visible').last();
    await itemDialog3.waitFor({ timeout: 8000 });
    await fillByLabel(p, itemDialog3, '单价', '99999999');
    await itemDialog3.locator('button:has-text("确 定")').click();
    await p.waitForTimeout(400);

    // 切回自购（切换前可能残留上次选择，先全部取消再点两人）+ 使用人
    await dialog.locator('.el-radio:visible', { hasText: '自购' }).first().click();
    await p.waitForTimeout(400);
    for (let i = 0; i < 5; i++) {
      const selected = await dialog.locator('.rp-card.is-selected:not(.is-disabled)').count();
      if (selected === 0) break;
      await dialog.locator('.rp-card.is-selected:not(.is-disabled)').first().click();
      await p.waitForTimeout(250);
    }
    await cards.nth(0).click();
    await p.waitForTimeout(200);
    await cards.nth(1).click();
    await p.waitForTimeout(300);
    await dialog.locator('.el-form-item', { hasText: '使用人' }).locator('.el-select__wrapper').first().click();
    await p.waitForTimeout(600);
    await p.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click();
    await p.waitForTimeout(300);

    // 标题 + 项目（保存草稿的必填项）
    await fillByLabel(p, dialog, '标题名称', '备用金冒烟测试');
    await treeSelectByLabel(p, dialog, '项目', ['超分辨光学显微镜']);

    // 保存草稿
    await dialog.locator('button:has-text("保 存 草 稿")').click();
    await p.waitForTimeout(1500);

    // 列表中找到该行 → 修改 → 验证组件回显（2 个序号 + 使用人已选）
    const row = p.locator('.el-table__row', { hasText: '备用金冒烟测试' }).first();
    await row.waitFor({ timeout: 10000 });
    await row.locator('button').nth(1).click();
    const editDialog = p.locator('.el-dialog:visible').last();
    await editDialog.waitFor({ timeout: 10000 });
    await p.waitForTimeout(800);
    const badges = await editDialog.locator('.rp-order').allInnerTexts();
    const useUserText = await editDialog.locator('.el-form-item', { hasText: '使用人' }).innerText();
    console.log('[check] 修改回显 badges =', JSON.stringify(badges), '，使用人 =', useUserText.trim().split('\n').pop());
    if (badges.length !== 2) throw new Error('修改回显备用金人数不对: ' + JSON.stringify(badges));
    if (!useUserText.includes('jvlit')) throw new Error('修改回显使用人缺失');
    await shot(p, '12-reserve-picker-edit');

    // 取消编辑 → 详情弹窗验证文本展示
    await editDialog.locator('button:has-text("取 消")').click();
    await p.waitForTimeout(600);
    await row.locator('button').nth(0).click();
    const detailDialog = p.locator('.el-dialog:visible').last();
    await detailDialog.waitFor({ timeout: 10000 });
    await p.waitForTimeout(800);
    const descText = await detailDialog.locator('.el-descriptions').innerText();
    console.log('[check] 详情含备用金人 =', descText.includes('备用金人'), '，含使用人 =', descText.includes('使用人'));
    // 只断言第一个选中人（jvlit），第二人随账户数据变化（如冯忠俊/彭赛威）不断言具体姓名
    if (!descText.includes('备用金人') || !descText.includes('jvlit') || !descText.includes('使用人')) throw new Error('详情备用金人/使用人展示缺失');
    await shot(p, '12-reserve-picker-detail');
    await detailDialog.locator('button:has-text("关 闭")').click();
    await p.waitForTimeout(500);

    // 清理：删除所有「备用金冒烟测试」草稿行（含历史遗留）
    for (let i = 0; i < 5; i++) {
      const draftRow = p.locator('.el-table__row', { hasText: '备用金冒烟测试' }).first();
      if (!(await draftRow.isVisible().catch(() => false))) break;
      await draftRow.locator('button').nth(3).click();
      await p.waitForTimeout(600);
      await p.locator('.el-message-box:visible button:has-text("确定")').click();
      await p.waitForTimeout(1000);
    }
    console.log('[check] 草稿已清理');

    console.log('[OK] reserve picker smoke passed');
    await browser.close();
  } catch (e) {
    await fail(p, e, 'reserve-picker');
  } finally {
    await ctx.close().catch(() => {});
  }
})();
