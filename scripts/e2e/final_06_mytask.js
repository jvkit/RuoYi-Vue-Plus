// 阶段2：fengzhongjun 我发起的 → 查看 → 详情页 → 审批记录-流程图（只读）
const { launch, login, shot, checkPageClean, fail } = require('./final_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    // 06 我发起的列表（含流程状态=审批中）
    await p.goto(`${BASE}/task/myDocument`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    const body = await p.locator('body').innerText();
    console.log('我发起的含采购申请审批:', body.includes('采购申请审批'));
    let chk = await checkPageClean(p);
    console.log('06 clean?', JSON.stringify(chk));
    await shot(p, '06-my-todo-applicant');

    // 点 查看 进详情
    const row = p.locator('.el-table__row', { hasText: '采购申请审批' }).first();
    await row.locator('button:has-text("查看")').click();
    await p.waitForURL(/request\/detail/, { timeout: 10000 });
    await p.waitForTimeout(3000);
    chk = await checkPageClean(p);
    console.log('06b clean?', JSON.stringify(chk));
    const dbody = await p.locator('body').innerText();
    for (const k of ['超分辨光学显微镜', '使用人', '采购明细', '精测-载玻片']) {
      console.log(`详情含「${k}」:`, dbody.includes(k));
    }
    await shot(p, '06b-request-detail');

    // 流程进度 → 审批记录弹窗 → 流程图（只读）
    await p.locator('button:has-text("流程进度")').first().click();
    await p.waitForTimeout(2000);
    await p.locator('.el-dialog:visible .el-tabs__item', { hasText: '流程图' }).first().click();
    await p.waitForTimeout(3500);
    chk = await checkPageClean(p);
    console.log('06c clean?', JSON.stringify(chk));
    await shot(p, '06c-request-flowchart');
    console.log('consoleErrors:', p.consoleErrors.slice(0, 5));
  } catch (e) {
    await fail(p, e, 'stage2');
  } finally {
    await b.close();
  }
})();
