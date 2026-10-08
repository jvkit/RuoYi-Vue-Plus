// 共享 helper：登录 + 常用选择器
// playwright 装在 scripts/demo-video/node_modules（该目录被 gitignore，但本地始终存在）
const { chromium } = require('/home/jvkit/workspace/oa/scripts/demo-video/node_modules/playwright');
const PIC = process.env.PIC || '/home/jvkit/workspace/oa/temp/testing/pic';
// BASE 统一从这里取：默认本地 dev；测试服务器用 OA_BASE=http://172.16.16.110/oa
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');

async function launch() {
  const b = await chromium.launch();
  return b;
}

async function login(browser, user, pass) {
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 1000 } });
  const p = await ctx.newPage();
  await p.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
  await p.locator('input[placeholder="用户名"]').fill(user);
  await p.locator('input[placeholder="密码"]').fill(pass);
  await p.locator('button:has-text("登 录")').click();
  await p.waitForURL(/\/index/, { timeout: 15000 });
  await p.waitForTimeout(1500);
  return { ctx, p };
}

// el-select 下拉选值（按 label 定位，兼容 filterable）
async function selectOptionByLabel(page, scope, label, text) {
  await openSelectByLabel(page, scope, label);
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: text }).first().click();
  await page.waitForTimeout(400);
}

// 按表单 label 精确定位表单项（label 元素可能是 label 或 div）
function formItemByLabel(label) {
  return `xpath=//div[contains(@class,'el-form-item')][*[contains(@class,'el-form-item__label') and normalize-space(.)='${label}']]`;
}

// 按 label 定位表单项内的输入框并填值
async function fillByLabel(page, scope, label, value) {
  const item = scope.locator(formItemByLabel(label)).first();
  await item.locator('input').first().fill(String(value));
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

// el-tree-select（label 定位版）：path 为从根到叶子的文本路径
async function treeSelectByLabel(page, scope, label, path) {
  await openSelectByLabel(page, scope, label);
  const pop = page.locator('.el-select-dropdown:visible, .el-popper:visible').last();
  for (let i = 0; i < path.length; i++) {
    const isLeaf = i === path.length - 1;
    if (!isLeaf) {
      // 中间节点：只负责展开（子节点不可见时点击展开图标）
      const node = pop.locator('.el-tree-node', { hasText: path[i] }).first();
      const child = node.locator('.el-tree-node').first();
      if (!(await child.isVisible().catch(() => false))) {
        await node.locator('.el-tree-node__expand-icon').first().click();
        await page.waitForTimeout(600);
      }
    } else {
      // 叶子：点其内容 span（EP 树默认插槽为裸 span，无 el-tree-node__label）
      await pop.locator('.el-tree-node__content span', { hasText: path[i] }).first().click();
      await page.waitForTimeout(500);
    }
  }
  await page.keyboard.press('Escape');
  await page.waitForTimeout(300);
}

// 允许手填的 el-select（allow-create）：按 label 打开下拉，输入后点下拉里的选项（无则回车创建）
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

async function shot(p, name) {
  await p.screenshot({ path: `${PIC}/${name}.png`, fullPage: true });
  console.log(`[shot] ${name}`);
}

async function fail(p, e, name) {
  await shot(p, 'FAIL-' + name);
  console.error('[FAIL]', name, e.message);
  throw e;
}

module.exports = { launch, login, selectOptionByLabel, treeSelectByLabel, fillByLabel, openSelectByLabel, selectAllowCreateByLabel, shot, fail, PIC };
