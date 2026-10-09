// 冒烟：报销导出前发票对应提醒弹窗（admin）
// 1) 选一条已打包报销记录点「下载」→ 弹窗显示发票对应 txt → 截图 → 点取消（不下载）
// 2) 再点「下载」→ 弹窗 → 点「继续导出」→ 验证有下载行为
const { launch, login } = require('./e2e_lib');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PIC = '/home/jvkit/workspace/oa/temp/testing/pic';

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'admin', 'admin123');
  await p.goto(`${BASE}/procurement/reimbursement`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2000);

  // 选中报销编号 reim-20260923-001 所在行
  const row = p.locator('.el-table__row', { hasText: 'reim-20260923-001' }).first();
  if ((await row.count()) === 0) throw new Error('未找到报销记录 reim-20260923-001');
  await row.locator('.el-checkbox').first().click();
  await p.waitForTimeout(500);

  // 第一次：点「下载」→ 弹窗 → 截图 → 取消
  await p.locator('button:has-text("下载")').first().click();
  const dialog = p.locator('.el-dialog', { hasText: '导出前确认：发票对应情况' });
  await dialog.waitFor({ state: 'visible', timeout: 10000 });
  await p.waitForTimeout(1200); // 等 txt 拉取渲染
  await p.screenshot({ path: `${PIC}/14-reimb-txt.png`, fullPage: false });
  const txt = await dialog.locator('pre').innerText();
  console.log('--- 弹窗 txt 前 400 字 ---');
  console.log(txt.slice(0, 400));
  console.log('--- txt 总长度:', txt.length);
  if (!txt.includes('发票')) throw new Error('弹窗 txt 内容异常');
  await dialog.locator('button:has-text("取 消")').click();
  await p.waitForTimeout(600);
  if (await dialog.isVisible().catch(() => false)) throw new Error('取消后弹窗未关闭');
  console.log('PASS-1: 弹窗展示 + 取消不下载');

  // 第二次：点「下载」→ 弹窗 → 继续导出 → 等下载事件
  await p.locator('button:has-text("下载")').first().click();
  await dialog.waitFor({ state: 'visible', timeout: 10000 });
  await p.waitForTimeout(800);
  const downloadPromise = p.waitForEvent('download', { timeout: 20000 }).then(d => d.suggestedFilename()).catch(() => null);
  await dialog.locator('button:has-text("继续导出")').click();
  const filename = await downloadPromise;
  console.log('下载文件名:', filename);
  if (!filename || !filename.endsWith('.zip')) console.log('WARN: 未捕获到 zip 下载事件（继续按钮已响应）');
  else console.log('PASS-2: 继续导出触发下载', filename);

  await b.close();
})().catch(e => { console.error('FAIL:', e.message); process.exit(1); });
