// 导演脚本：四窗口同屏 + 回车步进，重演 final 精测全链路（含新规则：自购必选备用金人）
// 用法：node director.js        （有头四窗口，按回车走下一步，q+回车退出）
//       PW_AUTO=1 node director.js （无头自动连跑，自测用）
const readline = require('readline');
const { chromium } = require('/home/jvkit/workspace/oa/scripts/demo-video/node_modules/playwright');
const { treeSelectByLabel } = require('./final_lib');

const AUTO = !!process.env.PW_AUTO;
const HEADED = !!process.env.PW_HEADED; // 有头自动连跑（不暂停，能看着窗口动）
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PHOTO = '/home/jvkit/workspace/oa/temp/testing/GX12_compressed.jpg';
const PAYMENT_IMG = '/home/jvkit/workspace/oa/test-invoices/good_invoice.png';

// 四位演员：2×2 自动摆位（左上 右上 / 左下 右下）
const CAST = {
  feng: { label: '冯忠俊·申请人(普通用户)', user: 'fengzhongjun', pos: [0, 0] },
  lidi: { label: '李迪·项目负责人', user: 'lidi', pos: [980, 0] },
  wjl: { label: '王建龙·CEO/采购经理', user: 'wangjianlong', pos: [0, 620] },
  ptz: { label: '裴天姿·仓库管理员', user: 'peitianzi', pos: [980, 620] }
};

const TITLE = '导演-显微镜载玻片采购';
const ITEM_SLIDE = { name: '导演-载玻片', spec: '75x25mm 帆船牌', unit: '盒', qty: 1, price: 100 };
const ITEM_COVER = { name: '导演-盖玻片', spec: '20x20mm 100片装', unit: '盒', qty: 1, price: 100 };
const AMOUNT = 200;

let stepNo = 0;
let rl = null;
function ask() {
  return new Promise((resolve) => {
    if (AUTO || HEADED) return resolve('');
    if (!rl) rl = readline.createInterface({ input: process.stdin, output: process.stdout });
    rl.question('', (ans) => resolve(ans));
  });
}
async function step(actor, action, look, expect) {
  stepNo++;
  console.log(`\n━━━━━━ 第 ${stepNo} 步 ━━━━━━`);
  console.log(`【${actor}】${action}`);
  if (look) console.log(`👀 看这里：${look}`);
  if (expect) console.log(`✅ 预期：${expect}`);
  if (!AUTO && !HEADED) {
    console.log('（检查完按回车执行这步；输入 q 回车退出）');
    const ans = await ask();
    if (ans.trim() === 'q') {
      console.log('👋 已退出，关闭所有窗口…');
      await cleanup();
      process.exit(0);
    }
  }
}

const browsers = [];
const P = {}; // actorKey -> page，供会话失效时按 key 重登
async function openActor(key) {
  const c = CAST[key];
  console.log(`[login] ${c.label} 开始…`);
  const browser = await chromium.launch({
    headless: AUTO && !HEADED ? true : false,
    args: [`--window-position=${c.pos[0]},${c.pos[1]}`, '--window-size=980,600']
  });
  browsers.push(browser);
  const ctx = await browser.newContext({ viewport: { width: 940, height: 540 } });
  const p = await ctx.newPage();
  p.on('response', (r) => {
    if (r.url().includes('/auth/login')) console.log(`[login] ${c.label} /auth/login -> ${r.status()}`);
  });
  await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  // vite 冷启动后 Vue 可能尚未水合，等登录按钮可用再操作
  await p.locator('button:has-text("登 录")').waitFor({ state: 'visible', timeout: 20000 });
  await p.waitForTimeout(1500);
  await p.locator('input[placeholder="用户名"]').fill(c.user);
  await p.locator('input[placeholder="密码"]').fill('666666');
  // 韧性登录：偶发 SPA 在 /auth/login 200 后卡住不进首页，手动 goto('/index') 推进路由守卫
  let ok = false;
  for (let attempt = 0; attempt < 3 && !ok; attempt++) {
    if (attempt > 0) console.log(`[login] ${c.label} 第 ${attempt + 1} 次尝试`);
    await p.locator('button:has-text("登 录")').click();
    await p.waitForTimeout(4000);
    if (!p.url().includes('/index')) {
      await p.goto(`${BASE}/index`, { waitUntil: 'domcontentloaded' }).catch(() => {});
      await p.waitForTimeout(4000);
    }
    ok = p.url().includes('/index');
  }
  if (!ok) throw new Error(`${c.label} 登录失败：多次尝试未进入首页，URL=${p.url()}`);
  await p.waitForTimeout(1200);
  console.log(`[login] ${c.label} 登录成功`);
  P[key] = p;
  return p;
}
/** 会话可能被同账号在别处登录挤掉：发现被弹回登录页就自动重登 */
async function ensureLogin(key) {
  const p = P[key];
  if (!p || !p.url().includes('/login')) return;
  const c = CAST[key];
  console.log(`[relogin] ${c.label} 会话失效（可能被同账号在别处登录挤出），自动重登…`);
  await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await p.locator('button:has-text("登 录")').waitFor({ state: 'visible', timeout: 20000 });
  await p.waitForTimeout(1200);
  await p.locator('input[placeholder="用户名"]').fill(c.user);
  await p.locator('input[placeholder="密码"]').fill('666666');
  for (let attempt = 0; attempt < 3; attempt++) {
    await p.locator('button:has-text("登 录")').click();
    await p.waitForTimeout(4000);
    if (!p.url().includes('/index')) {
      await p.goto(`${BASE}/index`, { waitUntil: 'domcontentloaded' }).catch(() => {});
      await p.waitForTimeout(4000);
    }
    if (p.url().includes('/index')) break;
  }
  if (p.url().includes('/login')) throw new Error(`${c.label} 重登失败，仍停留在登录页`);
  console.log(`[relogin] ${c.label} 重登成功`);
}
async function cleanup() {
  for (const b of browsers) await b.close().catch(() => {});
}

/* ---------- 通用小助手 ---------- */
async function gotoFundTab(key, tabName) {
  await ensureLogin(key);
  const p = P[key];
  await p.goto(`${BASE}/procurement/fund`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2000);
  await p.locator('.el-tabs__item', { hasText: tabName }).first().click();
  await p.waitForTimeout(2500);
}
async function openTodo(key, taskText) {
  await ensureLogin(key);
  const p = P[key];
  for (let attempt = 0; attempt < 3; attempt++) {
    await p.goto(`${BASE}/task/taskWaiting`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(3000);
    const row = p.locator('.el-table__row', { hasText: taskText }).first();
    if (await row.count()) {
      await row.locator('button:has-text("办理")').click();
      await p.waitForTimeout(3500);
      return;
    }
    console.log(`[todo] 未找到「${taskText}」行，刷新重试 (${attempt + 1}/3)`);
  }
  const body = (await p.locator('body').innerText()).slice(0, 400);
  throw new Error(`待办里找不到「${taskText}」。页面内容：${body}`);
}
async function passTodo(p) {
  await p.locator('button:has-text("审批")').first().click();
  await p.waitForTimeout(1500);
  const radio = p.locator('.el-dialog:visible .el-radio', { hasText: '通过' }).first();
  if (await radio.count()) { await radio.click(); await p.waitForTimeout(500); }
  await p.locator('.el-dialog:visible button:has-text("提交")').first().click();
  await p.waitForTimeout(2500);
  const c = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
  if (await c.count()) { await c.first().click(); await p.waitForTimeout(2000); }
}
async function confirmBox(p) {
  const c = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
  if (await c.count()) { await c.first().click(); await p.waitForTimeout(2200); }
}
const formItem = (label) => `xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='${label}']]`;

(async () => {
  console.log('🎬 导演脚本启动：即将打开 4 个窗口并依次登录四位演员');
  console.log('   布局：左上 冯忠俊 | 右上 李迪 | 左下 王建龙 | 右下 裴天姿');

  const feng = await openActor('feng');
  const lidi = await openActor('lidi');
  const wjl = await openActor('wjl');
  const ptz = await openActor('ptz');

  /* ========== 场 0：开局 ========== */
  await step('全员', '四个窗口已全部登录', '每个窗口左上角头像/右上角用户名', '冯忠俊(左上)、李迪(右上)、王建龙(左下)、裴天姿(右下)，各自首页正常无报错');

  /* ========== 场 1：资金基线（王建龙窗口） ========== */
  await gotoFundTab('wjl', '备用金视图');
  await step('王建龙', '打开 资金管理→备用金视图（已开好）', '左下窗口：账户卡片列表',
    '彭赛威 已用 3,073.97 / 可用 6,926.03；冯忠俊 已用 18,826.03 / 可用 −8,826.03（负数→卡片置灰）；陈俊荣 可用 3,426.03；jvlit 可用 10,000；何鑫 无账户不出现');

  await gotoFundTab('wjl', '项目视图');
  await step('王建龙', '切到 项目视图（已开好）', '项目树三列：预算/已用/剩余',
    '全域数字化平台 17,000,000/15,755.54/16,984,244.46；对内 4,000,000/15,755.54；对外 13,000,000/0；超分辨光学显微镜 5,000,000/11,900/4,988,100。父级已用=子级之和');

  await gotoFundTab('wjl', '资金状态看板');
  await step('王建龙', '切到 资金状态看板（已开好）', '状态分布卡片与明细', '各状态数量与明细行一致；存在历史「已采购未报销」单据');

  /* ========== 场 2：冯忠俊提交采购申请（自购·选彭赛威） ========== */
  await feng.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
  await feng.waitForTimeout(2500);
  await step('冯忠俊', '打开 采购申请列表（已开好）', '左上窗口列表页', '正常加载、无「没有访问权限」、无 500；注意列表已有历史单据');

  await feng.locator('button:has-text("新增")').first().click();
  await feng.waitForTimeout(1200);
  const dlg = feng.locator('.el-dialog:visible').first();
  await dlg.locator('.el-radio', { hasText: '自购' }).first().click();
  await dlg.locator('input[placeholder="如：笔记本购买"]').fill(TITLE);
  // 项目树（父级应不可选，选叶子：超分辨光学显微镜）——用 final 系列验证过的 treeSelectByLabel
  await treeSelectByLabel(feng, dlg, '项目', ['超分辨光学显微镜']);
  await dlg.locator('textarea[placeholder="请输入申请原因"]').fill('导演脚本端到端：显微镜实验耗材采购');
  await step('冯忠俊', '新增弹窗：选 自购 → 填名称 → 选项目（已操作完）', '弹窗上半部分',
    '采购方式=自购；申请标题不显示（后台自动生成）；项目树父级（全域/对内/对外）应置灰不可选，只能选叶子「超分辨光学显微镜」');

  // 明细 ×2
  const items = [ITEM_SLIDE, ITEM_COVER];
  for (let i = 0; i < items.length; i++) {
    const it = items[i];
    // 防御：如果上次残留明细弹窗（按 aria-label 精确定位，主弹窗叫「添加采购申请」不会误伤），先关掉
    const stray = feng.locator('div[role="dialog"][aria-label="添加明细"]:visible');
    if (await stray.count()) {
      await stray.locator('button:has-text("取 消"), button:has-text("取消")').first().click().catch(() => {});
      await feng.waitForTimeout(700);
    }
    await dlg.locator('button:has-text("添加明细")').first().click();
    await feng.waitForTimeout(1200);
    const idlg = feng.locator('div[role="dialog"][aria-label="添加明细"]:visible').last();
    await idlg.waitFor({ state: 'visible' });
    await treeSelectByLabel(feng, idlg, '分类', ['100材料']);
    await idlg.locator('input[placeholder="品名"]').fill(it.name);
    await idlg.locator('input[placeholder="规格型号"]').fill(it.spec);
    await idlg.locator(formItem('单位')).locator('input').first().fill(it.unit);
    await feng.keyboard.press('Enter');
    await feng.waitForTimeout(400);
    await idlg.locator(formItem('数量')).locator('input').first().fill(String(it.qty));
    await idlg.locator(formItem('单价')).locator('input').first().fill(String(it.price));
    await idlg.locator('textarea[placeholder*="该明细的采购原因"]').fill('导演脚本明细采购原因');
    if (i < items.length - 1) {
      await idlg.locator('button:has-text("再添加一个")').click();
      await feng.waitForTimeout(1200);
    } else {
      await idlg.locator('button:has-text("确 定")').click();
      await feng.waitForTimeout(800);
    }
  }
  await step('冯忠俊', '添加两条明细：载玻片 100 + 盖玻片 100（已操作完）', '弹窗明细表格',
    '每条 100元，合计 200元；分类显示 100材料/100材料（一级本身可作二级，默认）；物料用途默认「研发」');

  // 使用人 + 付款截图
  await dlg.locator(formItem('使用人')).locator('input').first().click();
  await feng.waitForTimeout(600);
  await dlg.locator(formItem('使用人')).locator('input').first().fill('冯忠俊');
  await feng.waitForTimeout(700);
  const useOpt = feng.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '冯忠俊' }).first();
  if (await useOpt.isVisible().catch(() => false)) { await useOpt.click(); } else { await feng.keyboard.press('Enter'); }
  await feng.waitForTimeout(400);
  await dlg.locator('input[type=file]').first().setInputFiles(PAYMENT_IMG);
  await feng.waitForTimeout(2500);
  await step('冯忠俊', '使用人=冯忠俊，上传付款截图（已操作完）', '弹窗下半部分',
    '付款截图缩略图出现（AI 识别成功显示提取金额，失败走兜底不报错）');

  // 【新规则拦截演示】不选备用金人直接提交
  await dlg.locator('button:has-text("提 交 申 请")').click();
  await feng.waitForTimeout(1200);
  const blockToast = feng.locator('.el-message--error', { hasText: '备用金出纳人' });
  const blocked = await blockToast.count() > 0;
  await step('冯忠俊', '【拦截演示】不选备用金人直接点提交（已操作完）', '弹窗顶部红字提示',
    `新规则：自购必须选人！应弹出「自购必须至少选择一个备用金出纳人（花谁的钱选谁）」→ 脚本检测到拦截提示：${blocked ? '✔ 已拦截' : '�’✘ 未拦截(问题!)'}`);

  // 选备用金人：彭赛威（冯忠俊自己负数置灰）
  await feng.locator('.rp-card', { hasText: '彭赛威' }).first().click();
  await feng.waitForTimeout(800);
  await step('冯忠俊', '备用金卡片点选 彭赛威（已操作完）', '备用金人卡片区',
    '彭赛威卡片出现序号「1」，其余人锁定置灰；冯忠俊卡片因可用为负(−8,826.03)本来就置灰；底部提示「1 号够用，已锁定」');

  await dlg.locator('button:has-text("提 交 申 请")').click();
  await feng.waitForTimeout(1500);
  await confirmBox(feng);
  await feng.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
  await feng.waitForTimeout(2500);
  const reqBody = await feng.locator('body').innerText();
  if (!reqBody.includes(TITLE)) throw new Error('采购申请提交后列表未见标题，提交可能失败');
  await step('冯忠俊', '再次提交 → 回列表（已操作完并核验）', '左上窗口列表第一行',
    `出现新单「【自购】超分辨光学显微镜_…_${TITLE}」，单号 purq-20260928-xxx，状态 waiting（审批中）✔`);

  // 审批前资金无变化
  await gotoFundTab('wjl', '备用金视图');
  await step('王建龙', '刷新备用金视图（已操作完）', '左下窗口',
    '【扣款时点】审批未完成前：彭赛威仍 已用 3,073.97 / 可用 6,926.03，无任何新流水——钱还没扣！');

  /* ========== 场 3：两级审批 ========== */
  await openTodo('lidi', '项目负责人审批');
  await step('李迪', '我的任务 → 点「办理」打开审批详情（已操作完）', '右上窗口审批详情页',
    '能看到申请人（冯忠俊）的全部材料：付款截图、明细、申请原因，无裂图、无「无权查看」；流程进度图正常');

  await passTodo(lidi);
  await step('李迪', '点 审批 → 选 通过 → 提交（已操作完）', '右上窗口', '提交成功；该单流转到 CEO（王建龙）');

  await openTodo('wjl', 'CEO审批');
  await step('王建龙', '我的任务 → 办理（已操作完）', '左下窗口审批详情', '同样能看到全部申请材料；金额 200 < 1000，流程下一步即 CEO → 结束（最高决策人暂跳过）');

  await passTodo(wjl);
  await gotoFundTab('wjl', '备用金视图');
  await step('王建龙', '审批通过 → 自动刷新备用金视图（已操作完）', '左下窗口账户卡片',
    '【扣款落地】彭赛威 已用 3,273.97 / 可用 6,726.03；自购流水多一条 FUND-20260928-xxx：出纳人=彭赛威、金额 200、申请编号=本单号');

  await gotoFundTab('wjl', '项目视图');
  await step('王建龙', '切 项目视图（已开好）', '项目树',
    '超分辨光学显微镜 已用 11,900→12,100 / 剩余 4,987,900；全域与对内不变（15,755.54）——备用金花项目必扣');

  await gotoFundTab('wjl', '资金状态看板');
  await step('王建龙', '切 资金状态看板（已开好）', '状态明细', `本单（${TITLE}）资金状态 = 已采购未报销`);

  /* ========== 场 4：采购验收（三级审批 + 自动入库） ========== */
  await feng.goto(`${BASE}/procurement/acceptance`, { waitUntil: 'networkidle' });
  await feng.waitForTimeout(2500);
  await feng.locator('button:has-text("新增")').first().click();
  await feng.waitForTimeout(1500);
  const adlg = feng.locator('.el-dialog:visible').first();
  await adlg.locator(formItem('关联采购')).locator('.el-select__wrapper, input').first().click();
  await feng.waitForTimeout(900);
  await feng.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: TITLE }).first().click();
  await feng.waitForTimeout(2500);
  const rows = adlg.locator('.el-table__row');
  for (let i = 0; i < (await rows.count()); i++) {
    await rows.nth(i).locator('input[type=file]').setInputFiles(PHOTO);
    await rows.nth(i).locator('.el-upload-list img, .el-upload-list__item, img').first().waitFor({ state: 'visible', timeout: 20000 });
    await feng.waitForTimeout(800);
  }
  await step('冯忠俊', '采购验收 → 新增：关联本采购申请，每个明细传 1 张实物图（已操作完）', '左上窗口验收弹窗',
    '下拉只列「未验收的已完成申请」；选中后自动带出 2 行明细（载玻片/盖玻片），每行传图成功显示缩略图');

  // 验收日期必填（final_10 有填，导演脚本首版漏了会导致前端校验拦下提交）
  await adlg.locator('input[placeholder="选择验收日期"]').fill('2026-09-28');
  await feng.keyboard.press('Enter');
  await feng.waitForTimeout(600);
  await adlg.locator('button:has-text("提 交")').click();
  await feng.waitForTimeout(1500);
  await confirmBox(feng);
  // 提交结果核验：列表必须出现新 acpt 单
  await feng.goto(`${BASE}/procurement/acceptance`, { waitUntil: 'networkidle' });
  await feng.waitForTimeout(2500);
  const acptBody = await feng.locator('body').innerText();
  const acptOk = /acpt-20260928-\d+/.test(acptBody);
  if (!acptOk) throw new Error('验收单提交后列表未出现 acpt-20260928 新单：可能前端校验拦截，检查弹窗必填项');
  await step('冯忠俊', '提交验收单（已操作完并核验）', '验收列表', '新验收单状态 waiting，单号 acpt-20260928-xxx ✔（脚本已正则核验存在）');

  await openTodo('feng', '采购申请人确认');
  await step('冯忠俊', '验收审批①：采购申请人确认 → 办理（已操作完）', '左上窗口', '能看到上一步传的 2 张实物图（无裂图）');

  await passTodo(feng);
  await openTodo('lidi', '项目负责人审批');
  await step('李迪', '验收审批②：项目负责人（李迪）→ 办理（已操作完）', '右上窗口', '同样能看到全部验收图片');
  await passTodo(lidi);
  await openTodo('wjl', '团队上级审批');
  await step('王建龙', '验收审批③：团队上级/CEO（王建龙）→ 办理（已操作完）', '左下窗口', '终审材料齐全');
  await passTodo(wjl);
  await step('全员', '验收终审通过（已操作完）', '—', '验收流程结束 → 商品【自动入库】（下一步看仓库）');

  /* ========== 场 5：仓库库存（裴天姿） ========== */
  await ptz.goto(`${BASE}/procurement/warehouse`, { waitUntil: 'networkidle' });
  await ptz.waitForTimeout(2500);
  await step('裴天姿', '打开 仓库库存（已开好）', '右下窗口', '列表出现 导演-载玻片(1盒) 和 导演-盖玻片(1盒)，来源=本验收单；物品图片列有图');
  const img = ptz.locator('.el-table__row', { hasText: '导演-载玻片' }).locator('img').first();
  if (await img.count()) { await img.click(); await ptz.waitForTimeout(1800); }
  await step('裴天姿', '点 导演-载玻片 的图片看大图（已操作完）', '右下窗口弹层', '大图正常显示不裂图（此前修过的存储桶图片问题）');

  /* ========== 场 6：领用 → 出库 ========== */
  await feng.goto(`${BASE}/procurement/issue`, { waitUntil: 'networkidle' });
  await feng.waitForTimeout(2500);
  await feng.locator('button:has-text("新增")').first().click();
  await feng.waitForTimeout(1200);
  const idlg = feng.locator('.el-dialog:visible').first();
  await idlg.locator(formItem('库存物料')).locator('.el-select__wrapper, input').first().click();
  await feng.waitForTimeout(900);
  await feng.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: '导演-载玻片' }).first().click();
  await feng.waitForTimeout(800);
  await idlg.locator(formItem('领用数量')).locator('input').first().fill('1');
  await idlg.locator('textarea[placeholder="请输入领用用途"]').fill('导演脚本：领用载玻片做实验');
  await step('冯忠俊', '领用申请 → 新增：选 导演-载玻片 ×1（已操作完）', '左上窗口领用弹窗', '库存物料下拉能搜到刚入库的载玻片；领用数量 1');

  await idlg.locator('button:has-text("提 交")').click();
  await feng.waitForTimeout(1500);
  await confirmBox(feng);
  await feng.goto(`${BASE}/procurement/issue`, { waitUntil: 'networkidle' });
  await feng.waitForTimeout(2500);
  const issueBody = await feng.locator('body').innerText();
  if (!/issue-20260928-\d+/.test(issueBody)) throw new Error('领用申请提交后列表未见 issue-20260928 新单，提交可能失败');
  await step('冯忠俊', '提交领用申请（已操作完并核验）', '领用列表', '新单 waiting，单号 issue-20260928-xxx ✔');

  await openTodo('lidi', '项目负责人');
  await step('李迪', '领用审批①：项目负责人（李迪）→ 办理（已操作完）', '右上窗口', '领用详情正常（品名/数量/用途）');
  await passTodo(lidi);
  await openTodo('ptz', '仓库管理员');
  await step('裴天姿', '领用审批②：仓库管理员（裴天姿）→ 办理（已操作完）', '右下窗口', '审批通过即【出库】');
  await passTodo(ptz);

  await ptz.goto(`${BASE}/procurement/warehouse`, { waitUntil: 'networkidle' });
  await ptz.waitForTimeout(2500);
  await step('裴天姿', '刷新仓库库存（已开好）', '右下窗口', '导演-载玻片 数量 1 → 0（已出库）；导演-盖玻片 仍为 1');

  /* ========== 终场 ========== */
  console.log('\n🎬 全剧终！四窗口保持打开，可自由翻看。');
  console.log('📋 本剧未覆盖（后续可补）：');
  console.log('   ① 上传发票（AI匹配/拖拽/草稿）+ 发票台账');
  console.log('   ② 报销导出 zip + 发票对应提醒 + 状态翻转');
  console.log('   ③ 对公申请（需报价单+开票抬头税号）');
  console.log('   ④ 驳回→重新提交流');
  console.log('   ⑤ 越权直访 404（/procurement/invoice、/fund、/project）');
  console.log('   ⑥ 手机端验收 /mobile/acceptance');
  console.log('   ⑦ 资金管理人工登记（自购/对公两条线）');
  await cleanup();
  process.exit(0);
})().catch(async (e) => {
  console.error('\n💥 演出事故：', e.message);
  await cleanup();
  process.exit(1);
});
