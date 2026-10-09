// 铃铛通知测试：验证「普通用户提交申请 → 项目负责人收到实时弹窗 + 铃铛红点」
// 用法：node notice_test.js            （有头，冯忠俊 + 李迪两个窗口）
//       PW_AUTO=1 node notice_test.js （无头）
const { chromium } = require('/home/jvkit/workspace/oa/scripts/demo-video/node_modules/playwright');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const AUTO = !!process.env.PW_AUTO;

const APPLIER = { user: 'fengzhongjun', pass: '666666', label: '冯忠俊·申请人' };
const LEADER = { user: 'lidi', pass: '666666', label: '李迪·项目负责人' };
// 与项目表一致：超分辨光学显微镜 的 leader_id = 李迪
const PROJECT_NAME = '超分辨光学显微镜';
const ITEM = { name: '铃铛测试-载玻片', spec: '75x25mm', unit: '盒', qty: 1, price: 50 };

async function openWindow(browser, who, pos) {
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 900 } });
  const p = await ctx.newPage();
  await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await p.locator('input[placeholder="用户名"]').fill(who.user);
  await p.locator('input[placeholder="密码"]').fill(who.pass);
  await p.evaluate(() => {
    const btns = document.querySelectorAll('button');
    for (const b of btns) { if (b.textContent.replace(/\s/g, '').includes('登录')) { b.click(); return; } }
  });
  await p.waitForURL(/\/index/, { timeout: 15000 });
  await p.waitForTimeout(1500);
  if (pos && !AUTO) {
    // 有头模式摆位（忽略 headless 下的窗口位置）
    try { await p.evaluate(() => window.moveTo(0, 0)); } catch {}
  }
  console.log(`[login] ${who.label} 登录成功`);
  return { ctx, p };
}

async function markAllRead(p) {
  await p.locator('.message-trigger').click();
  await p.waitForTimeout(800);
  const btn = p.locator('.head-box-btn', { hasText: '全部已读' });
  if (await btn.count()) { await btn.first().click(); await p.waitForTimeout(500); }
  // 关掉 popover
  await p.keyboard.press('Escape');
  await p.waitForTimeout(300);
  console.log('[leader] 已执行全部已读');
}

async function getUnreadBadge(p) {
  const badge = p.locator('.message-trigger .el-badge__content').first();
  if (!(await badge.count())) return 0;
  const txt = await badge.innerText().catch(() => '0');
  const n = parseInt(txt, 10);
  return isNaN(n) ? (txt.includes('+') ? 99 : 0) : n;
}

async function submitRequest(applierPage) {
  const p = applierPage;
  await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2000);
  await p.evaluate(() => {
    const btns = document.querySelectorAll('button');
    for (const b of btns) { if (b.textContent.replace(/\s/g, '').includes('新增')) { b.click(); return; } }
  });
  await p.waitForTimeout(1500);

  // 表单（对公，避开备用金/付款截图等自购必填项）
  const dlg = p.locator('.el-dialog:visible').first();
  await dlg.locator('.el-radio', { hasText: '对公' }).first().click();
  await p.waitForTimeout(400);
  await dlg.locator('xpath=//div[contains(@class,"el-form-item")][.//*[normalize-space()="标题名称"]]//input').first().fill(ITEM.name);
  // 项目（树选择，点叶子）
  const projItem = dlg.locator('//div[contains(@class,"el-form-item")][.//*[normalize-space()="项目"]]').first();
  await projItem.locator('.el-form-item__content').first().click({ force: true });
  await p.waitForTimeout(1000);
  await p.waitForTimeout(800);
  const spans = await p.locator('.el-tree-node__content span').all();
  for (const sp of spans) {
    if (await sp.isVisible() && (await sp.innerText()).includes(PROJECT_NAME)) {
      await sp.click({ force: true });
      break;
    }
  }
  await p.waitForTimeout(500);
  await p.waitForTimeout(400);
  // 采购种类
  await p.evaluate(() => {
    const app = document.querySelector('#app').__vue_app__;
    let found = null;
    const walk = (inst) => {
      if (!inst || found) return;
      if (inst.form) { found = inst; return; }
      const children = inst.subTree?.children;
      if (Array.isArray(children)) {
        for (const c of children) {
          if (c && typeof c === 'object' && c.component) walk(c.component);
          if (c && typeof c === 'object' && c.children) walk({ subTree: c });
        }
      }
    };
    walk(app._instance);
    if (found) found.form.purchaseType = '科研类';
  });
  await p.waitForTimeout(300);
  await p.waitForTimeout(600);
  await p.locator('.el-select-dropdown__item', { hasText: '科研类' }).first().evaluate(el => el.click());
  await p.waitForTimeout(300);
  // 申请原因
  await dlg.locator('xpath=//div[contains(@class,"el-form-item")][.//*[normalize-space()="申请原因"]]//textarea').first().fill('铃铛通知测试');

  // 明细
  await p.evaluate(() => {
    const btns = document.querySelectorAll('button');
    for (const b of btns) { if (b.textContent.replace(/\s/g, '').includes('添加明细')) { b.click(); return; } }
  });
  await p.waitForTimeout(800);
  await p.waitForTimeout(500);
  const dialog = p.locator('.el-dialog:visible').last();
  await dialog.locator('//div[contains(@class,"el-form-item")][.//*[normalize-space()="品名"]]//input').fill(ITEM.name);
  await dialog.locator('//div[contains(@class,"el-form-item")][.//*[normalize-space()="规格"]]//input').fill(ITEM.spec);
  // 单位（下拉）
  await dialog.locator('//div[contains(@class,"el-form-item")][.//*[normalize-space()="单位"]]').first().locator('.el-form-item__content').first().click({ force: true });
  await p.waitForTimeout(500);
  await p.locator('.el-select-dropdown__item', { hasText: ITEM.unit }).first().evaluate(el => el.click());
  await p.waitForTimeout(300);
  // 数量/单价（input-number）
  await dialog.locator('//div[contains(@class,"el-form-item")][.//*[normalize-space()="数量"]]//input').fill(String(ITEM.qty));
  await dialog.locator('//div[contains(@class,"el-form-item")][.//*[normalize-space()="单价"]]//input').fill(String(ITEM.price));
  await p.evaluate(() => {
    const btns = document.querySelectorAll('button');
    for (const b of btns) { if (b.offsetParent !== null && b.textContent.replace(/\s/g, '').includes('确定') && b.textContent.replace(/\s/g, '').length < 5) { b.click(); return; } }
  });
  await p.waitForTimeout(800);

  // 提交
  const errors = await p.evaluate(() => {
    const dlg = document.querySelector('.el-dialog');
    if (!dlg) return 'no dialog';
    const errs = dlg.querySelectorAll('.el-form-item__error');
    return Array.from(errs).map(e => e.textContent);
  });

  // 保存草稿（绕过表单 validate），再从列表提交（触发流程）
  await p.evaluate(() => {
    const btns = document.querySelectorAll('button');
    for (const b of btns) { if (b.offsetParent !== null && b.textContent.replace(/\s/g, '').includes('保存草稿')) { b.click(); return; } }
  });
  await p.waitForTimeout(2500);
  await p.screenshot({ path: '/tmp/before_submit.png' });

  // 列表里找草稿行，点提交按钮
  await p.waitForTimeout(1500);
  const rowCount = await p.locator('.el-table__row', { hasText: ITEM.name }).count();
  const row = p.locator('.el-table__row', { hasText: ITEM.name }).first();
  if (rowCount > 0) {
  }
  await row.locator('button').first().waitFor({ state: 'visible', timeout: 5000 });
  // 操作列按钮顺序：查看(0) 修改(1) 提交(2) 删除(3) 导出(4)
  const btns = await row.locator('button:not([disabled])').all();
  if (btns.length >= 3) {
    await btns[2].click({ force: true });
  } else if (btns.length >= 2) {
    await btns[1].click({ force: true });
  }
  await p.waitForTimeout(1200);
  const confirmBtn = p.locator('.el-message-box button:has-text("确定"), .el-message-box button:has-text("确认"), .el-message-box button:has-text("确 定")').first();
  await p.waitForTimeout(3000);
  console.log('[applier] 已提交申请');
}

(async () => {
  const browser = await chromium.launch({ headless: AUTO });
  let allOk = true;

  // 1. 打开两个窗口
  const leader = await openWindow(browser, LEADER, [0, 0]);
  const applier = await openWindow(browser, APPLIER, [960, 0]);

  // 2. 领导先全部已读，清零铃铛
  await markAllRead(leader.p);
  const before = await getUnreadBadge(leader.p);
  console.log(`[check] 领导铃铛未读数（提交前）: ${before}`);

  // 3. 申请人提交
  await submitRequest(applier.p);

  // 4. 领导侧等通知（SSE 实时，最多等 8 秒）
  let notifAppeared = false;
  for (let i = 0; i < 40; i++) {
    const n = await getUnreadBadge(leader.p);
    const notif = await leader.p.locator('.el-notification:visible').count();
    if (n > before || notif > 0) {
      notifAppeared = true;
      console.log(`[check] 领导侧检测到通知（badge=${n}, 弹窗=${notif}）`);
      break;
    }
    await leader.p.waitForTimeout(500);
  }

  // 5. 截图 + 结论
  await leader.p.screenshot({ path: '/tmp/notice_leader.png', clip: { x: 0, y: 0, width: 940, height: 620 } });
  if (notifAppeared) {
    console.log('✅ 铃铛通知测试通过：领导实时收到新申请提醒');
  } else {
    const after = await getUnreadBadge(leader.p);
    console.log(`❌ 铃铛通知测试失败：提交后未检测到通知（badge 前=${before} 后=${after}）`);
    allOk = false;
  }

  await browser.close();
  process.exit(allOk ? 0 : 1);
})().catch(e => { console.error('💥 测试异常:', e.message.split('\n')[0]); process.exit(1); });
