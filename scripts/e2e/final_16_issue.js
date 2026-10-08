// 阶段8：fengzhongjun 领用申请（选精测-载玻片 ×1）→ 提交 → 列表
const { launch, login, shot, checkPageClean, fail, selectOptionByLabel, fillByLabel } = require('./final_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    await p.goto(`${BASE}/procurement/issue`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);

    await p.locator('button:has-text("新增")').first().click();
    await p.waitForTimeout(1200);
    const dlg = p.locator('.el-dialog:visible').first();
    await selectOptionByLabel(p, dlg, '库存物料', '精测-载玻片');
    await p.waitForTimeout(800);
    // 领用数量必填，填 1
    await fillByLabel(p, dlg, '领用数量', 1);
    await p.waitForTimeout(300);
    await dlg.locator('textarea[placeholder="请输入领用用途"]').fill('精测端到端测试：领用载玻片做实验');
    // 拉高视口截完整弹窗
    await p.setViewportSize({ width: 1600, height: 1200 });
    await p.waitForTimeout(500);
    const chk = await checkPageClean(p);
    console.log('16 clean?', JSON.stringify(chk));
    await p.screenshot({ path: require('./final_lib').FINAL + '/16-issue-add.png' });
    console.log('[shot] 16-issue-add');

    // 提交（先回到 1000 高，保证提交按钮在可视区）
    await p.setViewportSize({ width: 1600, height: 1000 });
    await dlg.locator('button:has-text("提 交")').click();
    await p.waitForTimeout(1500);
    const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
    if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2500); }

    // 17 列表
    await p.goto(`${BASE}/procurement/issue`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    await p.locator('input[placeholder*="品名"]').fill('精测');
    await p.locator('button:has-text("搜索")').first().click();
    await p.waitForTimeout(2000);
    const body = await p.locator('body').innerText();
    console.log('领用列表含精测:', body.includes('精测-载玻片'));
    console.log('17 clean?', JSON.stringify(await checkPageClean(p)));
    await shot(p, '17-issue-list');
  } catch (e) {
    await fail(p, e, 'stage8');
  } finally {
    await b.close();
  }
})();
