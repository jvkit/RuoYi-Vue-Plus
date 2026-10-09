// 品牌改造验证：登录页截图 + admin 登录后报销/资金/发票工作台三页 404 复查
const { chromium } = require('playwright');
const fs = require('fs');

const BASE = (process.env.OA_BASE || process.env.BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const OUT = process.env.OUT || '/home/jvkit/workspace/oa/temp/rebrand_check';
fs.mkdirSync(OUT, { recursive: true });

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1600, height: 900 } });
  const page = await ctx.newPage();

  // 1. 登录页
  await page.goto(BASE + '/login', { waitUntil: 'networkidle' });
  await page.screenshot({ path: OUT + '/01-login.png' });
  console.log('title:', await page.title());

  // 2. 登录 admin
  await page.fill('input[placeholder*="账号"], input[placeholder*="用户名"]', 'admin');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button.submit-button');
  await page.waitForURL((u) => !u.pathname.includes('/login'), { timeout: 15000 });
  await page.waitForTimeout(2500);
  await page.screenshot({ path: OUT + '/02-home.png' });

  // 3. 三个曾 404 的页面
  const targets = [
    ['reimbursement', '/procurement/reimbursement'],
    ['fund', '/procurement/fund'],
    ['invoiceWorkbench', '/procurement/invoiceWorkbench']
  ];
  for (const [name, path] of targets) {
    await page.goto(BASE + path, { waitUntil: 'networkidle' }).catch(() => {});
    await page.waitForTimeout(2000);
    const bodyText = await page.textContent('body');
    const is404 = bodyText.includes('404') && bodyText.length < 3000;
    console.log(name, '=>', is404 ? 'STILL 404' : 'OK', 'url:', page.url());
    await page.screenshot({ path: `${OUT}/03-${name}.png` });
  }

  await browser.close();
})().catch((e) => { console.error(e.message); process.exit(1); });
