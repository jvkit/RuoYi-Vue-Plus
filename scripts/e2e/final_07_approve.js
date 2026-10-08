// 阶段3：lidi(项目负责人审批) → wangjianlong(CEO审批) 依次通过
const { launch, login, shot, checkPageClean, fail, approveTodo } = require('./final_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
(async () => {
  const b = await launch();
  const steps = [
    { user: 'lidi', shotName: '07-approve-lidi', task: '项目负责人审批' },
    { user: 'wangjianlong', shotName: '08-approve-ceo', task: 'CEO审批' }
  ];
  for (const s of steps) {
    const { ctx, p } = await login(b, s.user, '666666');
    try {
      const found = await approveTodo(p, s.task);
      console.log(s.shotName, '待办找到:', found);
      await p.goto(`${BASE}/task/taskFinish`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(2500);
      const chk = await checkPageClean(p);
      console.log(s.shotName, 'clean?', JSON.stringify(chk));
      await shot(p, s.shotName);
    } catch (e) {
      await fail(p, e, s.shotName);
    } finally {
      await ctx.close().catch(() => {});
    }
  }
  await b.close();
})();
