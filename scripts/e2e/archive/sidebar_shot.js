// 侧栏配色对比截图：登录 admin → 首页截图（fresh context，不受 localStorage 缓存影响）
const { chromium } = require('playwright');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const OUT = process.argv[2] || '/home/jvkit/workspace/oa/temp/rebrand_check/sidebar.png';

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 900 } });
  const page = await ctx.newPage();
  await page.goto(BASE + '/login', { waitUntil: 'networkidle' });
  await page.fill('input[placeholder*="账号"], input[placeholder*="用户名"]', 'admin');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button.submit-button');
  await page.waitForURL((u) => !u.pathname.includes('/login'), { timeout: 15000 });
  // 展开采购管理，让侧栏内容更丰富
  await page.waitForTimeout(2000);
  const menu = page.locator('.el-sub-menu__title', { hasText: '采购管理' });
  if (await menu.count()) await menu.first().click();
  await page.waitForTimeout(800);
  await page.screenshot({ path: OUT });
  console.log('saved', OUT);
  await browser.close();
})().catch((e) => { console.error(e.message); process.exit(1); });
