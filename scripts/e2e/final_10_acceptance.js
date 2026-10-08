// 阶段5：fengzhongjun 采购验收列表 → 新增验收（关联精测申请、逐行传实物图）→ 提交
const { launch, login, shot, checkPageClean, fail, selectOptionByLabel } = require('./final_lib');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PHOTO = '/home/jvkit/workspace/oa/temp/testing/GX12_compressed.jpg';

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    // 10 验收列表
    await p.goto(`${BASE}/procurement/acceptance`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    let chk = await checkPageClean(p);
    console.log('10 clean?', JSON.stringify(chk));
    await shot(p, '10-acceptance-list');

    // 新增
    await p.locator('button:has-text("新增")').first().click();
    await p.waitForTimeout(1500);
    const dlg = p.locator('.el-dialog:visible').first();
    await selectOptionByLabel(p, dlg, '关联采购申请', '精测-显微镜载玻片采购');
    await p.waitForTimeout(2500);

    // 每行上传实物图（GX12 小图）
    const rows = dlg.locator('.el-table__row');
    const n = await rows.count();
    console.log('明细行数:', n);
    for (let i = 0; i < n; i++) {
      const row = rows.nth(i);
      await row.locator('input[type=file]').setInputFiles(PHOTO);
      await row.locator('.el-upload-list img, .el-upload-list__item, img').first().waitFor({ state: 'visible', timeout: 20000 });
      await p.waitForTimeout(800);
    }
    // 验收日期
    await dlg.locator('input[placeholder="选择验收日期"]').fill('2026-09-24');
    await p.keyboard.press('Enter');
    await p.waitForTimeout(600);
    // 视口拉高再截，确保弹窗完整
    await p.setViewportSize({ width: 1600, height: 1500 });
    await p.waitForTimeout(500);
    await p.evaluate(() => { window.scrollTo(0, 0); });
    chk = await checkPageClean(p);
    console.log('11 clean?', JSON.stringify(chk));
    await p.screenshot({ path: require('./final_lib').FINAL + '/11-acceptance-add.png' });
    console.log('[shot] 11-acceptance-add');

    // 提交
    await p.setViewportSize({ width: 1600, height: 1000 });
    await dlg.locator('button:has-text("提 交")').click();
    await p.waitForTimeout(1500);
    const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
    if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2500); }
    const body = await p.locator('body').innerText();
    console.log('验收列表含精测:', body.includes('精测'));
    console.log('consoleErrors:', p.consoleErrors.slice(0, 3));
  } catch (e) {
    await fail(p, e, 'stage5');
  } finally {
    await b.close();
  }
})();
