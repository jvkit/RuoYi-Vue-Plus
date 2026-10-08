// E2E-15 阶段2：李迪(项目负责人审批) → 王建龙(CEO审批) → 流程 finish 触发拆账
// 申请标题关键字：拆账联测；实例 2103025722108858369
const { execSync } = require('child_process');
const { launch, login, shot, fail } = require('./e2e_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const REQ_ID = '2103025721328717826';
const INSTANCE_ID = '2103025722108858369';

function flowState() {
  const sql = `SELECT t.node_name, t.flow_status FROM flow_task t WHERE t.instance_id=${INSTANCE_ID} AND t.del_flag='0' ORDER BY t.id DESC LIMIT 1`;
  const out = execSync(`docker exec -i mysql mysql -uroot -proot -D "ry-vue-6x" --default-character-set=utf8mb4 -e "${sql}" 2>/dev/null`).toString();
  return out.replace(/\s+/g, ' ').trim();
}

const APPROVERS = [
  { user: 'lidi', name: '李迪', prefix: '15-e2e-05', taskName: '项目负责人审批' },
  { user: 'wangjianlong', name: '王建龙', prefix: '15-e2e-07', taskName: 'CEO审批' }
];

(async () => {
  const b = await launch();
  console.log('[db] 审批前流程状态:', flowState());
  for (const ap of APPROVERS) {
    const { ctx, p } = await login(b, ap.user, '666666');
    try {
      await p.goto(`${BASE}/task/taskWaiting`, { waitUntil: 'networkidle' });
      await p.waitForTimeout(2500);
      await shot(p, ap.prefix + '-1-task-waiting');
      const row = p.locator('.el-table__row', { hasText: '冯忠俊' }).locator(`xpath=self::*[contains(.,'${ap.taskName}')]`).first();
      if (!(await row.count())) {
        const fallback = p.locator('.el-table__row', { hasText: ap.taskName }).first();
        if (await fallback.count()) { await fallback.locator('button:has-text("办理")').click(); }
        else {
          console.log(ap.prefix, ap.name, '无「拆账联测/冯忠俊」待办，dump 页面文本前300字:');
          console.log((await p.locator('body').innerText()).replace(/\n+/g, ' | ').slice(0, 300));
          await shot(p, ap.prefix + '-no-task');
          await ctx.close();
          continue;
        }
      } else {
        await row.locator('button:has-text("办理")').click();
      }
      await p.waitForTimeout(3500);
      await shot(p, ap.prefix + '-2-approval-page');

      await p.locator('button:has-text("审批")').first().click();
      await p.waitForTimeout(1500);
      const passRadio = p.locator('.el-dialog:visible .el-radio', { hasText: '通过' }).first();
      if (await passRadio.count()) { await passRadio.click(); await p.waitForTimeout(500); }
      await p.locator('.el-dialog:visible button:has-text("提交")').first().click();
      await p.waitForTimeout(2500);
      const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
      if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2000); }
      await shot(p, ap.prefix + '-3-approved');
      console.log(ap.prefix, ap.name, '审批提交完成; [db] 当前流程状态:', flowState());
    } catch (e) {
      await fail(p, e, ap.prefix);
    } finally {
      await ctx.close().catch(() => {});
    }
  }
  await b.close();
  console.log('[OK] stage2 approvals done');
})();
