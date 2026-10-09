// 侧栏配色候选对比：登录 admin，注入 CSS 覆盖侧栏背景，裁剪侧栏区域截图
const { chromium } = require('playwright');
const fs = require('fs');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const OUT = '/home/jvkit/workspace/oa/temp/rebrand_check/colors';
fs.mkdirSync(OUT, { recursive: true });

const LIGHT_TEXT_CSS = (bg) => `
  #app .sidebar-shell{border-color:#e5e7eb !important}
  #app .sidebar-shell .el-scrollbar.theme-dark{--side-menu-text:#1f2937;--side-menu-hover-bg:rgba(64,158,255,.08);--side-menu-hover-text:#111827;--side-menu-active-bg:rgba(64,158,255,.12);--side-menu-active-text:#356dff;--side-menu-active-border:rgba(64,158,255,.16);--side-menu-active-line:#356dff;}
  #app .sidebar-shell .el-menu-item, #app .sidebar-shell .el-sub-menu__title{color:#1f2937 !important}
  #app .sidebar-shell .el-menu-item.is-active, #app .sidebar-shell .is-active > .el-sub-menu__title{color:#356dff !important}
  #app .sidebar-logo-link{background:#f8fafc !important;border-color:#e5e7eb !important}
  #app .sidebar-title{color:#1f2937 !important}
`;

const VARIANTS = [
  { name: '1-deepblue-current', bg: '#162a4f' },
  { name: '2-midnight', bg: '#101a30' },
  { name: '3-slate', bg: '#2e3a4e' },
  { name: '4-teal', bg: '#113a3a' },
  { name: '5-white', bg: '#ffffff', textColor: '#1f2937', css: LIGHT_TEXT_CSS('#ffffff') },
  { name: '6-iceblue', bg: '#eef3fb', textColor: '#1f2937', css: LIGHT_TEXT_CSS('#eef3fb') }
];

(async () => {
  const browser = await chromium.launch();
  const only = process.argv[2] ? process.argv[2].split(',') : null;
  for (const v of VARIANTS) {
    if (only && !only.some((o) => v.name.startsWith(o))) continue;
    const ctx = await browser.newContext({ viewport: { width: 1600, height: 900 } });
    const page = await ctx.newPage();
    await page.goto(BASE + '/login', { waitUntil: 'networkidle' });
    await page.fill('input[placeholder*="账号"], input[placeholder*="用户名"]', 'admin');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button.submit-button');
    await page.waitForURL((u) => !u.pathname.includes('/login'), { timeout: 15000 });
    await page.waitForTimeout(1800);
    const css = v.css || '';
    if (css) await page.addStyleTag({ content: css });
    await page.evaluate(
      ([bg, textColor]) => {
        const shell = document.querySelector('.sidebar-shell');
        if (shell) {
          shell.style.setProperty('background', bg, 'important');
          if (textColor) shell.style.setProperty('--el-menu-text-color', textColor, 'important');
        }
      },
      [v.bg, v.textColor || null]
    );
    const menu = page.locator('.el-sub-menu__title', { hasText: '采购管理' });
    if (await menu.count()) await menu.first().click();
    await page.waitForTimeout(600);
    await page.screenshot({ path: `${OUT}/${v.name}.png`, clip: { x: 0, y: 0, width: 250, height: 900 } });
    console.log('saved', v.name);
    await ctx.close();
  }
  await browser.close();
})().catch((e) => { console.error(e.message); process.exit(1); });
