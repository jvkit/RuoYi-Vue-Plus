// E2E 阶段3b：验收单审批链 冯忠俊(采购申请人) -> 李迪(项目负责人) -> 王建龙(团队上级)
const { launch, login, shot, fail } = require('./e2e_lib');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const APPROVERS = [
  { user: 'fengzhongjun', prefix: '03-5', name: '冯忠俊-采购申请人' },
  { user: 'lidi', prefix: '03-6', name: '李迪-项目负责人' },
  { user: 'wangjianlong', prefix: '03-7', name: '王建龙-团队上级' },
];
(async () => {
  const b = await launch();
  for (const ap of APPROVERS) {
    const { ctx, p } = await login(b, ap.user, '666666');
    try {
      await p.goto(`${BASE}/task/taskWaiting`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(2500);
      const row = p.locator('.el-table__row').first();
      if (!(await row.count())) { console.log(ap.prefix, ap.name, '无待办'); await shot(p, ap.prefix + '-no-task'); await ctx.close(); continue; }
      console.log(ap.prefix, '任务:', (await row.innerText()).replace(/\n+/g, ' | ').slice(0, 150));
      await row.locator('button:has-text("办理")').click();
      await p.waitForTimeout(3500);
      await shot(p, ap.prefix + '-approval-page');
      await p.locator('button:has-text("审批")').first().click();
      await p.waitForTimeout(1500);
      const passRadio = p.locator('.el-dialog:visible .el-radio', { hasText: '通过' }).first();
      if (await passRadio.count()) { await passRadio.click(); await p.waitForTimeout(500); }
      await p.locator('.el-dialog:visible button:has-text("提交")').first().click();
      await p.waitForTimeout(2500);
      const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
      if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2000); }
      await shot(p, ap.prefix + '-approved');
      console.log(ap.prefix, ap.name, '审批完成');
    } catch (e) { await fail(p, e, ap.prefix); }
    finally { await ctx.close().catch(() => {}); }
  }
  await b.close();
})();
