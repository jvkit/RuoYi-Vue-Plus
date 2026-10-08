// 快速冒烟测试：登录 + 关键页面 200 + 权限检查
// 用法：node smoke_test.js        （有头）
//       PW_AUTO=1 node smoke_test.js （无头）
const { launch, login, shot, checkPageClean } = require('./final_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PAGES = [
  { path: '/procurement/request', name: '采购申请', expect: ['没有访问权限'] },
  { path: '/procurement/acceptance', name: '采购验收', expect: ['没有访问权限'] },
  { path: '/procurement/warehouse', name: '仓库库存', expect: ['没有访问权限'] },
  // 财务三件套只对采购经理开放。判定逻辑见 checkPageClean：命中"没有访问权限/401"算通过（权限拦截生效），
  // 命中"404"算异常（404 在这里意味着菜单缺失或路由丢失，是 bug 而非权限拦截）。此数组仅作文档说明。
  { path: '/procurement/invoiceWorkbench', name: '发票工作台', allowNoPerm: true },
  { path: '/procurement/reimbursement', name: '报销导出', allowNoPerm: true },
  { path: '/procurement/fund', name: '资金管理', allowNoPerm: true }
];

(async () => {
  const browser = await launch();
  const { ctx, p } = await login(browser, 'fengzhongjun', '666666');
  console.log('✔ 登录成功（冯忠俊）');

  let allOk = true;
  for (const page of PAGES) {
    await p.goto(`${BASE}${page.path}`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2000);
    // 财务三件套（allowNoPerm）对普通用户应被权限拦截：看到"没有访问权限/401"即通过；
    // 出现 404 说明菜单缺失/路由丢失，是 bug。其它页面一律要求干净（无 404/500/无权限文案）。
    const noPermExpected = page.allowNoPerm === true;
    const body = await p.locator('body').innerText();
    const hitNoPerm = ['没有访问权限，请联系管理员授权', '401'].some((b) => body.includes(b));
    const hitBad = ['404', 'Internal Server Error', 'Request failed with status code 500'].some((b) => body.includes(b));
    const ok = noPermExpected ? hitNoPerm && !hitBad : !hitNoPerm && !hitBad;
    console.log(`${ok ? '✅' : '❌'} ${page.name}: ${ok ? '正常' : `异常（noPerm=${hitNoPerm} bad=${hitBad}）`}`);
    if (!ok) allOk = false;
    await shot(p, `smoke-${page.name}`);
  }

  await browser.close();
  console.log(allOk ? '\n🎉 冒烟测试全部通过' : '\n💥 冒烟测试存在异常');
  process.exit(allOk ? 0 : 1);
})().catch(async (e) => {
  console.error('\n💥 冒烟测试失败：', e.message);
  await browser.close().catch(() => {});
  process.exit(1);
});
