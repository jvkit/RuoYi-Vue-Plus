// 重截 02-menu v2：关弹窗 → 确保侧边栏展开（按 DOM 宽度校验）→ 展开子菜单
const { launch, login, shot, checkPageClean } = require('./final_lib');

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    // 关闭右上角系统消息弹层
    await p.keyboard.press('Escape');
    await p.locator('.el-notification__closeBtn, .el-message-box__close, [class*="close"]').first().click().catch(() => {});
    await p.waitForTimeout(500);
    // 确保侧边栏展开：宽度 < 100 视为收起，点 hamburger 切换
    for (let i = 0; i < 3; i++) {
      const w = await p.evaluate(() => {
        const el = document.querySelector('.sidebar-container') || document.querySelector('.left-panel');
        return el ? el.getBoundingClientRect().width : 0;
      });
      console.log('sidebar width =', w);
      if (w >= 100) break;
      await p.locator('.hamburger-container').first().click();
      await p.waitForTimeout(1000);
    }
    // 只展开「采购管理」（unique-opened 互斥，展开一个会收起其它）
    const t = p.locator('.el-sub-menu__title span', { hasText: '采购管理' }).first();
    if (await t.count()) { await t.click().catch(() => {}); await p.waitForTimeout(800); }
    const chk = await checkPageClean(p);
    console.log('02 clean?', JSON.stringify(chk));
    await shot(p, '02-menu');
  } finally {
    await b.close();
  }
})();
