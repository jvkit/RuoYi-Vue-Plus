// final 精测共享 helper：截图到 output/final，失败截图到 /tmp/final-debug
// PW_STEP=1 进入分步调试：有头浏览器 + 每张截图后自动 page.pause()，
// 终端打印看点/预期，检查完在 Playwright Inspector 点 Resume（F8）继续下一步。
const { chromium } = require('/home/jvkit/workspace/oa/scripts/demo-video/node_modules/playwright');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const FINAL = '/home/jvkit/workspace/oa/temp/testing/output/final';
const DEBUG = '/tmp/final-debug';

/** 各截图点的看点与预期（分步调试时打印到终端） */
const STEP_HINTS = {
  '01-login': '登录后首页（fengzhongjun 普通用户视角）：菜单应只有 首页/PRIME AI/工作流(我的任务)/采购管理 等，无系统管理/资金管理/项目管理',
  '02-menu': '菜单展开：采购管理下应见 采购申请/采购验收/仓库库存/领用申请/BOM物料库/BOM表(产品)/流转记录；注意不该出现的菜单',
  '03-request-list': '采购申请列表：正常加载出数据，无「没有访问权限」无 500',
  '04-request-add': '新增表单：①备用金卡片中冯忠俊可用为负应置灰、点不中；②分类树 100材料 本身可点（默认 100/100）；③标题后台自动生成不显示；④付款截图已上传',
  '05-request-submitted': '提交成功：列表出现「精测-显微镜载玻片采购」，单号 purq-日期-序号，状态 waiting；此时资金管理应无任何变化',
  '06b-request-detail': '申请详情：明细/备用金人(应为空，冯忠俊置灰未选上)/使用人 等字段齐全',
  '06c-request-flowchart': '流程图：start→apply→leader→ceo→end 渲染正常（控制台 NaN 告警是上游组件问题，忽略）',
  '07-approve-lidi': '李迪审批页：办理窗口能看到申请人全部材料，无裂图、无「无权查看」',
  '08-approve-ceo': '王建龙 CEO 审批页（金额<1000 走 ceo→end，最高决策人暂跳过）',
  '09-request-finished': '申请状态 = finish（已完成）；资金管理此时才出现扣款',
  '10-acceptance-list': '采购验收列表：正常加载',
  '11-acceptance-add': '验收新增：关联采购申请下拉只列「未验收的已完成申请」，每个明细可传实物照片',
  '12-acceptance-approve-self': '验收发起人自审：上一步传的实物照片必须可见',
  '13-acceptance-approve-lidi': '李迪验收审批：同样应能看到全部验收图片',
  '14-acceptance-approve-ceo': '王建龙验收终审',
  '15-warehouse-stock': '仓库库存：验收完成的物品已自动入库，物品图片点击放大不裂图',
  '16-issue-add': '领用新增：库存页只读可见（普通用户无入库/编辑按钮），领用数量可选',
  '17-issue-list': '领用申请列表：新单状态 waiting',
  '18-issue-approve': '李迪领用审批',
  '19-issue-done': '裴天姿仓库审批通过（出库）',
  '20-stock-after-issue': '出库后库存：被领物品数量相应扣减',
  '21-mobile': '手机端验收页 /mobile/acceptance：待验收单可见',
  '22-invoice-noaccess-check': '何鑫(普通用户)侧边栏：不应出现 发票管理/资金管理/项目管理',
  '22b-invoice-404': '普通用户直访 /procurement/invoice：应 404/无权',
  '23-fund-404': '普通用户直访 /procurement/fund：应 404/无权'
};

async function launch() {
  // 分步调试必须有头，方便人眼看页面
  return chromium.launch({ headless: !process.env.PW_STEP });
}

async function login(browser, user, pass) {
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 1000 } });
  const p = await ctx.newPage();
  // 页面报错收集（500/权限提示在 body 里也能看到，这里额外抓 console error）
  p.consoleErrors = [];
  p.on('console', (msg) => {
    if (msg.type() === 'error') p.consoleErrors.push(msg.text());
  });
  await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await p.locator('input[placeholder="用户名"]').fill(user);
  await p.locator('input[placeholder="密码"]').fill(pass);
  await p.locator('button:has-text("登 录")').click();
  await p.waitForURL(/\/index/, { timeout: 15000 });
  await p.waitForTimeout(1800);
  return { ctx, p };
}

// fullPage 截图会触发侧栏渲染成收起态的怪癖，改为：量文档高度→拉高视口→视口截图
async function tallShot(p, dir, name) {
  const height = await p.evaluate(() => Math.max(document.documentElement.scrollHeight, document.body.scrollHeight, 1000));
  const h = Math.min(Math.ceil(height) + 20, 4000);
  await p.setViewportSize({ width: 1600, height: h });
  await p.waitForTimeout(600);
  await p.screenshot({ path: `${dir}/${name}.png`, fullPage: false });
  await p.setViewportSize({ width: 1600, height: 1000 });
  console.log(`[shot] ${name} (h=${h})`);
  // 分步调试：每张截图后暂停，人在浏览器里检查，点 Inspector Resume 继续
  if (process.env.PW_STEP) {
    const hint = STEP_HINTS[name] || '检查当前页面是否符合预期';
    console.log(`\n====== [停] ${name} ======\n看点：${hint}\n检查完点 Inspector 的 Resume（快捷键 F8）继续\n`);
    await p.pause();
  }
}

async function shot(p, name) {
  await tallShot(p, FINAL, name);
}

async function shotDebug(p, name) {
  await tallShot(p, DEBUG, name);
}

/** 检查页面是否出现无权限/报错文案 */
async function checkPageClean(p) {
  const body = await p.locator('body').innerText();
  const bad = ['没有访问权限，请联系管理员授权', '404', 'Internal Server Error', 'Request failed with status code 500'];
  const found = bad.filter((b) => body.includes(b));
  return { ok: found.length === 0, found, consoleErrors: p.consoleErrors || [] };
}

function formItemByLabel(label) {
  return `xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='${label}']]`;
}

async function openSelectByLabel(page, scope, label) {
  const item = scope.locator(formItemByLabel(label)).first();
  for (let attempt = 0; attempt < 3; attempt++) {
    await item.locator('.el-select__wrapper, input').first().click();
    await page.waitForTimeout(900);
    const n = await page.locator('.el-popper:visible').count();
    if (n > 0) return;
  }
  throw new Error(`openSelectByLabel(${label}) 弹层未打开`);
}

async function selectOptionByLabel(page, scope, label, text) {
  await openSelectByLabel(page, scope, label);
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: text }).first().click();
  await page.waitForTimeout(400);
}

async function fillByLabel(page, scope, label, value) {
  const item = scope.locator(formItemByLabel(label)).first();
  await item.locator('input').first().fill(String(value));
}

async function treeSelectByLabel(page, scope, label, path) {
  await openSelectByLabel(page, scope, label);
  const pop = page.locator('.el-select-dropdown:visible, .el-popper:visible').last();
  for (let i = 0; i < path.length; i++) {
    const isLeaf = i === path.length - 1;
    if (!isLeaf) {
      const node = pop.locator('.el-tree-node', { hasText: path[i] }).first();
      const child = node.locator('.el-tree-node').first();
      if (!(await child.isVisible().catch(() => false))) {
        await node.locator('.el-tree-node__expand-icon').first().click();
        await page.waitForTimeout(600);
      }
    } else {
      await pop.locator('.el-tree-node__content span', { hasText: path[i] }).first().click();
      await page.waitForTimeout(500);
    }
  }
  await page.keyboard.press('Escape');
  await page.waitForTimeout(300);
}

async function selectAllowCreateByLabel(page, scope, label, text) {
  const item = scope.locator(formItemByLabel(label)).first();
  await item.locator('.el-select__wrapper, input').first().click();
  await page.waitForTimeout(600);
  await item.locator('input').first().fill(text);
  await page.waitForTimeout(700);
  const opt = page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: text }).first();
  if (await opt.isVisible().catch(() => false)) {
    await opt.click();
  } else {
    await page.keyboard.press('Enter');
  }
  await page.waitForTimeout(400);
  await page.keyboard.press('Escape');
}

/** 办理待办：在 /task/taskWaiting 找含 taskText 的行点办理→审批→通过→提交 */
async function approveTodo(p, taskText) {
  await p.goto(`${BASE}/task/taskWaiting`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);
  const row = p.locator('.el-table__row', { hasText: taskText }).first();
  if (!(await row.count())) return false;
  await row.locator('button:has-text("办理")').click();
  await p.waitForTimeout(3500);
  await p.locator('button:has-text("审批")').first().click();
  await p.waitForTimeout(1500);
  const passRadio = p.locator('.el-dialog:visible .el-radio', { hasText: '通过' }).first();
  if (await passRadio.count()) { await passRadio.click(); await p.waitForTimeout(500); }
  await p.locator('.el-dialog:visible button:has-text("提交")').first().click();
  await p.waitForTimeout(2500);
  const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
  if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2000); }
  return true;
}

async function fail(p, e, name) {
  await shotDebug(p, 'FAIL-' + name);
  console.error('[FAIL]', name, e.message);
  throw e;
}

module.exports = {
  launch, login, shot, shotDebug, checkPageClean, fail,
  selectOptionByLabel, fillByLabel, openSelectByLabel, treeSelectByLabel, selectAllowCreateByLabel,
  approveTodo, FINAL, DEBUG
};
