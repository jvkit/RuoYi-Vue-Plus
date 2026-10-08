// 报销专项测试：选已验收单 → 生成报销包 → 导出 zip
// 用法：node reimburse_test.js        （有头导演模式）
//       PW_AUTO=1 node reimburse_test.js （无头自动连跑）
const { launch, login, shot, shotDebug, fail, FINAL, DEBUG, selectOptionByLabel, approveTodo, checkPageClean } = require('./final_lib');
const fs = require('fs');
const path = require('path');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const TITLE = '报销专项-淘宝件采购';

let stepNo = 0;
function step(actor, action, look, expect) {
  stepNo++;
  console.log(`\n━━━━━━ 第 ${stepNo} 步 ━━━━━━`);
  console.log(`【${actor}】${action}`);
  if (look) console.log(`👀 看这里：${look}`);
  if (expect) console.log(`✅ 预期：${expect}`);
}

(async () => {
  const browser = await launch();
  const { ctx, p } = await login(browser, 'wangjianlong', '666666');

  step('王建龙', '登录成功，开始报销专项测试');
  await shot(p, '01-login');

  // ========== 报销导出 ==========
  await p.goto(`${BASE}/procurement/reimbursement`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  await shot(p, '02-reimbursement-list');
  const listCheck = await checkPageClean(p);
  if (!listCheck.ok) console.warn('[warn] 报销导出列表有异常文案:', listCheck.found);

  // 生成报销包
  await p.locator('button:has-text("生成报销包")').first().click();
  await p.waitForTimeout(1500);
  const packDlg = p.locator('.el-dialog:visible').first();
  await packDlg.waitFor({ state: 'visible' });

  // 选择关联申请（按标题搜索）
  await packDlg.locator('.el-select__wrapper, input').first().click();
  await p.waitForTimeout(900);
  const opt = p.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: TITLE }).first();
  if (!(await opt.count())) throw new Error(`报销打包下拉未找到 ${TITLE}，请确认该单已验收完成`);
  await opt.click();
  await p.waitForTimeout(1500);
  await shot(p, '03-pack-dialog');
  step('王建龙', '选择关联申请', '生成报销包弹窗', `项目/申请人自动带出，申请=${TITLE}`);

  await packDlg.locator('button:has-text("打 包")').click();
  await p.waitForTimeout(5000);
  const packOk = p.locator('.el-message--success', { hasText: '报销包生成成功' });
  if (!(await packOk.count())) throw new Error('报销包生成未提示成功');
  await shot(p, '04-pack-success');
  step('王建龙', '点击打包', '列表', '出现新报销记录，状态=已打包，打包文件列有「查看」链接');

  // 刷新列表，勾选并下载（Playwright 会拦截浏览器下载）
  await p.goto(`${BASE}/procurement/reimbursement`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  const row = p.locator('.el-table__row', { hasText: TITLE }).first();
  if (!(await row.count())) throw new Error(`报销列表未找到 ${TITLE}`);
  await row.locator('.el-checkbox').first().click();
  await p.waitForTimeout(500);
  const [download] = await Promise.all([
    p.waitForEvent('download', { timeout: 15000 }),
    p.locator('button:has-text("下载")').first().click()
  ]);
  await p.waitForTimeout(3000);

  // 处理发票对应提醒弹窗
  const invoiceDlg = p.locator('.el-dialog:visible').first();
  if (await invoiceDlg.count()) {
    await shot(p, '05-invoice-txt-dialog');
    step('王建龙', '弹出发票对应提醒', '发票对应情况文本', '显示该申请的发票匹配明细，只提醒不阻止');
    await invoiceDlg.locator('button:has-text("继续导出")').click();
    await p.waitForTimeout(5000);
  }
  await shot(p, '06-download-success');
  step('王建龙', '继续导出', '浏览器下载', 'zip 文件下载成功，文件名=reim-xxx.zip');

  // 保存 zip 到本地
  const zipPath = path.join(process.cwd(), `${download.suggestedFilename()}`);
  await download.saveAs(zipPath);
  const stat = fs.statSync(zipPath);
  console.log(`\n[zip 已保存] ${zipPath} (${stat.size} bytes)`);

  // ========== 资金状态看板 ==========
  await p.goto(`${BASE}/procurement/fund`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  await p.locator('.el-tabs__item', { hasText: '资金状态看板' }).first().click();
  await p.waitForTimeout(2500);
  await shot(p, '07-fund-status-board');
  step('王建龙', '查看资金状态看板', '报销状态分布', `${TITLE} 应显示为 已报销未汇款`);

  console.log('\n🎉 报销专项测试完成！');
  await browser.close();
  process.exit(0);
})().catch(async (e) => {
  console.error('\n💥 报销专项测试失败：', e.message);
  await browser.close().catch(() => {});
  process.exit(1);
});
