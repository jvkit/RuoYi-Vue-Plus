// 发票专项测试：一条采购 → 验收 → 两批 PDF AI 识别 → 补传 → 台账核对
// 用法：node invoice_test.js        （有头导演模式）
//       PW_AUTO=1 node invoice_test.js （无头自动连跑）
const { launch, login, shot, shotDebug, fail, FINAL, DEBUG, selectOptionByLabel, treeSelectByLabel, approveTodo, checkPageClean } = require('./final_lib');
const fs = require('fs');
const path = require('path');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PHOTO = '/home/jvkit/workspace/oa/temp/testing/GX12_compressed.jpg';
const TEST_INVOICE_DIR = '/home/jvkit/workspace/oa/test-invoices';
const PURCHASE_MATERIAL_DIR = '/home/jvkit/workspace/oa/temp/testing/采购素材(1)/采购素材/8.0105淘宝件验收图/8.0105采购发票';

const TITLE = '发票专项-淘宝件采购';
const ITEM_NAMES = ['加热带', 'typec双公头定制', 'XH2.54_4P'];
const ITEM_SPECS = ['1卷', '定制线', '4P'];
const ITEM_UNITS = ['卷', '根', '包'];
const ITEM_QTYS = [1, 1, 1];
const ITEM_PRICES = [70, 120, 5.9];
const AMOUNT = ITEM_PRICES.reduce((a, b) => a + b, 0);

const BATCH1_FILES = ['1-加热带-70.00元.pdf', '3-XH2.54_4P-5.90元.pdf', '7-内存卡发票-415.pdf'];
const BATCH2_FILES = ['2-typec双公头定制-120.00元.pdf', '8.Type-C母座发票-6.6.pdf'];

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
  const { ctx, p } = await login(browser, 'fengzhongjun', '666666');
  const wjl = await login(browser, 'wangjianlong', '666666');
  const lidi = await login(browser, 'lidi', '666666');

  step('冯忠俊', '登录成功，开始发票专项测试');
  await shot(p, '01-login');

  // ========== 采购申请 ==========
  await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  await shot(p, '02-request-list');
  const listCheck = await checkPageClean(p);
  if (!listCheck.ok) console.warn('[warn] 采购申请列表有异常文案:', listCheck.found);

  await p.locator('button:has-text("新增")').first().click();
  await p.waitForTimeout(1200);
  const dlg = p.locator('.el-dialog:visible').first();
  await dlg.locator('.el-radio', { hasText: '自购' }).first().click();
  await dlg.locator('input[placeholder="如：笔记本购买"]').fill(TITLE);
  await treeSelectByLabel(p, dlg, '项目', ['超分辨光学显微镜']);
  await dlg.locator('textarea[placeholder="请输入申请原因"]').fill('发票专项测试：淘宝件采购，验证发票AI识别与补传');

  for (let i = 0; i < ITEM_NAMES.length; i++) {
    // 防御：如果上次残留明细弹窗，先关掉
    const stray = p.locator('div[role="dialog"][aria-label="添加明细"]:visible');
    if (await stray.count()) {
      await stray.locator('button:has-text("取 消"), button:has-text("取消")').first().click().catch(() => {});
      await p.waitForTimeout(700);
    }
    await dlg.locator('button:has-text("添加明细")').first().click();
    await p.waitForTimeout(1200);
    const idlg = p.locator('div[role="dialog"][aria-label="添加明细"]:visible').last();
    await idlg.waitFor({ state: 'visible' });
    await treeSelectByLabel(p, idlg, '分类', ['100材料']);
    await idlg.locator('input[placeholder="品名"]').fill(ITEM_NAMES[i]);
    await idlg.locator('input[placeholder="规格型号"]').fill(ITEM_SPECS[i]);
    await idlg.locator(`xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='单位']]`).locator('input').first().fill(ITEM_UNITS[i]);
    await p.keyboard.press('Enter');
    await p.waitForTimeout(400);
    await idlg.locator(`xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='数量']]`).locator('input').first().fill(String(ITEM_QTYS[i]));
    await idlg.locator(`xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='单价']]`).locator('input').first().fill(String(ITEM_PRICES[i]));
    await idlg.locator('textarea[placeholder*="该明细的采购原因"]').fill('发票专项明细');
    if (i < ITEM_NAMES.length - 1) {
      await idlg.locator('button:has-text("再添加一个")').click();
      await p.waitForTimeout(1200);
    } else {
      await idlg.locator('button:has-text("确 定")').click();
      await p.waitForTimeout(800);
    }
  }
  step('冯忠俊', '添加 3 条明细', '弹窗明细表格', `每条金额 ${ITEM_PRICES.join(' / ')}，合计 ${AMOUNT} 元`);

  await dlg.locator(`xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='使用人']]`).locator('input').first().click();
  await p.waitForTimeout(600);
  await dlg.locator(`xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='使用人']]`).locator('input').first().fill('冯忠俊');
  await p.waitForTimeout(700);
  const useOpt = p.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '冯忠俊' }).first();
  if (await useOpt.isVisible().catch(() => false)) { await useOpt.click(); } else { await p.keyboard.press('Enter'); }
  await p.waitForTimeout(400);
  await dlg.locator('input[type=file]').first().setInputFiles(PHOTO);
  await p.waitForTimeout(2500);

  // 备用金人：选彭赛威（第一个测试账号）
  await p.locator('.rp-card', { hasText: '彭赛威' }).first().click();
  await p.waitForTimeout(800);
  step('冯忠俊', '选备用金人=彭赛威，上传付款截图', '备用金卡片区', '彭赛威出现序号 1，其余锁定');

  await dlg.locator('button:has-text("提 交 申 请")').click();
  await p.waitForTimeout(1500);
  const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
  if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2000); }
  await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  const reqBody = await p.locator('body').innerText();
  if (!reqBody.includes(TITLE)) throw new Error('采购申请提交后列表未见标题');
  await shot(p, '03-request-submitted');
  step('冯忠俊', '提交采购申请', '采购申请列表', `出现新单「${TITLE}」，状态 waiting`);

  // ========== 审批 ==========
  await approveTodo(lidi.p, '项目负责人审批');
  step('李迪', '审批通过：项目负责人', '—', '流转到 CEO');
  await approveTodo(wjl.p, 'CEO审批');
  step('王建龙', '审批通过：CEO', '—', '流程结束，资金落地');
  await p.waitForTimeout(2000);

  // ========== 验收 ==========
  await p.goto(`${BASE}/procurement/acceptance`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  await p.locator('button:has-text("新增")').first().click();
  await p.waitForTimeout(1500);
  const adlg = p.locator('.el-dialog:visible').first();
  await adlg.locator(`xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='关联采购申请']]`).locator('.el-select__wrapper, input').first().click();
  await p.waitForTimeout(900);
  await p.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: TITLE }).first().click();
  await p.waitForTimeout(2500);
  const rows = adlg.locator('.el-table__row');
  for (let i = 0; i < (await rows.count()); i++) {
    await rows.nth(i).locator('input[type=file]').setInputFiles(PHOTO);
    await rows.nth(i).locator('.el-upload-list img, .el-upload-list__item, img').first().waitFor({ state: 'visible', timeout: 20000 });
    await p.waitForTimeout(800);
  }
  await adlg.locator('input[placeholder="选择验收日期"]').fill('2026-09-29');
  await p.keyboard.press('Enter');
  await p.waitForTimeout(600);
  await adlg.locator('button:has-text("提 交")').click();
  await p.waitForTimeout(1500);
  const cbtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
  if (await cbtn.count()) { await cbtn.first().click(); await p.waitForTimeout(2000); }
  await p.goto(`${BASE}/procurement/acceptance`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  const acptBody = await p.locator('body').innerText();
  if (!/acpt-20260929-\d+/.test(acptBody)) throw new Error('验收单提交后列表未出现 acpt-20260929 新单');
  await shot(p, '04-acceptance-submitted');
  step('冯忠俊', '提交验收单', '验收列表', `新单 acpt-20260929-xxx，状态 waiting`);

  // 验收审批
  await approveTodo(p, '采购申请人确认');
  step('冯忠俊', '验收审批①：采购申请人确认', '—', '通过');
  await approveTodo(lidi.p, '项目负责人审批');
  step('李迪', '验收审批②：项目负责人', '—', '通过');
  await approveTodo(wjl.p, '团队上级审批');
  step('王建龙', '验收审批③：团队上级', '—', '通过，验收完成');
  await p.waitForTimeout(2000);

  // ========== 发票工作台 ==========
  // 权限：冯忠俊无 procurement:workbench:list，改用王建龙（采购经理）操作
  const wjlP = wjl.p;
  await wjlP.goto(`${BASE}/procurement/invoiceWorkbench`, { waitUntil: 'networkidle' });
  await wjlP.waitForTimeout(2500);
  await shot(wjlP, '05-invoice-workbench');
  step('王建龙', '打开发票上传工作台', '工作台列表', `找到本单「${TITLE}」，状态应为 未上传/正在上传`);

  // 找到本单行的「上传发票」按钮
  const wbRow = wjlP.locator('.el-table__row', { hasText: TITLE }).first();
  if (!(await wbRow.count())) throw new Error(`发票工作台未找到 ${TITLE}`);
  await wbRow.locator('button:has-text("上传发票")').click();
  await wjlP.waitForTimeout(2000);

  const matchDlg = wjlP.locator('.el-dialog:visible').first();
  await matchDlg.waitFor({ state: 'visible' });
  await shot(wjlP, '06-invoice-match-dialog');
  step('王建龙', '打开发票匹配台', '匹配台弹窗', '左侧 3 条商品明细，右侧发票池为空');

  // 第一批：3 张 PDF（2 有效 + 1 无关）
  console.log(`\n[批次1] 上传 ${BATCH1_FILES.length} 张 PDF：匹配=加热带/XH2.54，无关=内存卡`);
  const batch1Paths = BATCH1_FILES.map(f => path.join(PURCHASE_MATERIAL_DIR, f));
  // 顶部上传区是第一个 el-upload（multiple），明细行内的直传是单独的
  await matchDlg.locator('.match-upload input[type=file]').setInputFiles(batch1Paths);
  await wjlP.waitForTimeout(1500);
  await matchDlg.locator('button:has-text("开始识别（AI匹配）")').click();
  await wjlP.waitForTimeout(5000);
  step('王建龙', '第一批 AI 识别', '识别结果区', '应显示 3 条识别报告：2 张已匹配、1 张无关/未匹配');
  await shot(wjlP, '07-ai-match-batch1');

  // 等待识别完成
  for (let i = 0; i < 60; i++) {
    const done = await matchDlg.locator('.ai-report').count();
    if (done) break;
    await wjlP.waitForTimeout(2000);
  }
  await shot(wjlP, '08-ai-match-batch1-done');
  step('王建龙', '第一批识别完成', '发票池', '发票池应出现 3 张卡片，2 张已挂载、1 张未匹配（内存卡）');

  // 第二批：2 张 PDF（1 有效 + 1 无关）
  console.log(`\n[批次2] 上传 ${BATCH2_FILES.length} 张 PDF：匹配=typec，无关=Type-C母座`);
  const batch2Paths = BATCH2_FILES.map(f => path.join(PURCHASE_MATERIAL_DIR, f));
  await matchDlg.locator('.match-upload input[type=file]').setInputFiles(batch2Paths);
  await wjlP.waitForTimeout(1500);
  await matchDlg.locator('button:has-text("开始识别（AI匹配）")').click();
  await wjlP.waitForTimeout(5000);
  step('王建龙', '第二批 AI 识别', '识别结果区', '应显示 2 条识别报告：1 张已匹配、1 张无关/未匹配');
  await shot(wjlP, '09-ai-match-batch2');

  for (let i = 0; i < 60; i++) {
    const done = await matchDlg.locator('.ai-report').count();
    if (done) break;
    await wjlP.waitForTimeout(2000);
  }
  await shot(wjlP, '10-ai-match-batch2-done');
  step('王建龙', '第二批识别完成', '发票池', '发票池应共 5 张卡片，3 张已挂载、2 张未匹配（内存卡/Type-C母座）');

  // 补传：把未匹配的内存卡发票拖拽到加热带明细（模拟人工修正 AI 错误匹配）
  console.log('\n[补传] 将 7-内存卡发票-415.pdf 拖拽挂载到 加热带 明细行');
  const memCard = matchDlg.locator('.invoice-card', { hasText: '内存卡' }).first();
  const heaterRow = matchDlg.locator('.item-card', { hasText: '加热带' }).first();
  const cardBox = await memCard.boundingBox();
  const rowBox = await heaterRow.boundingBox();
  if (cardBox && rowBox) {
    await wjlP.mouse.move(cardBox.x + cardBox.width / 2, cardBox.y + cardBox.height / 2);
    await wjlP.mouse.down();
    await wjlP.mouse.move(rowBox.x + rowBox.width / 2, rowBox.y + rowBox.height / 2, { steps: 20 });
    await wjlP.mouse.up();
    await wjlP.waitForTimeout(2000);
  }
  await shot(wjlP, '11-manual-fix-memcard');
  step('王建龙', '拖拽补传：内存卡发票 → 加热带明细', '发票池', '内存卡卡片应显示「已挂载」到加热带');

  // 直接上传 Type-C母座发票到 typec 明细（测试单文件直传兜底，不走 AI）
  console.log('\n[直传] 上传 8.Type-C母座发票-6.6.pdf 到 typec双公头定制 明细行');
  const typecRow = matchDlg.locator('.item-card', { hasText: 'typec双公头定制' }).first();
  const fileInput = typecRow.locator('input[type=file]');
  await fileInput.setInputFiles(path.join(PURCHASE_MATERIAL_DIR, '8.Type-C母座发票-6.6.pdf'));
  await wjlP.waitForTimeout(3000);
  await shot(wjlP, '12-direct-upload-typec');
  step('王建龙', '直接上传 Type-C母座发票到 typec 明细', '发票池', '发票池新增 1 张发票，挂载到 typec（金额不匹配但允许挂载）');

  // 关闭匹配台，查看工作台状态
  await matchDlg.locator('button:has-text("关 闭")').click();
  await wjlP.waitForTimeout(1500);
  await wjlP.goto(`${BASE}/procurement/invoiceWorkbench`, { waitUntil: 'networkidle' });
  await wjlP.waitForTimeout(2500);
  const wbBody = await wjlP.locator('body').innerText();
  const statusMatch = wbBody.match(new RegExp(`${TITLE}[\\s\\S]{0,300}`));
  console.log('\n[工作台状态]', statusMatch ? statusMatch[0].slice(0, 200) : '未找到');
  await shot(wjlP, '13-workbench-after');
  step('王建龙', '刷新发票工作台', '状态列', '本单状态应为 正在上传 或 已完成，明细覆盖 3/3，发票总数 7 张（5 AI + 2 人工）');

  // ========== 发票台账 ==========
  await wjlP.goto(`${BASE}/procurement/invoice`, { waitUntil: 'networkidle' });
  await wjlP.waitForTimeout(2500);
  await shot(wjlP, '14-invoice-ledger');
  step('王建龙', '打开发票台账', '台账列表', `应能看到本单 ${TITLE} 的发票记录，含无效/有效标签`);

  console.log('\n🎉 发票专项测试完成！');
  await browser.close();
  process.exit(0);
})().catch(async (e) => {
  console.error('\n💥 发票专项测试失败：', e.message);
  process.exit(1);
});
