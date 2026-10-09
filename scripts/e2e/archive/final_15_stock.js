// 阶段7：fengzhongjun 仓库库存（精测物品已入库）+ 点开实物大图验证
const { launch, login, shot, checkPageClean, fail, FINAL } = require('./final_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    await p.goto(`${BASE}/procurement/warehouse`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    // 按品名过滤精测
    await p.locator('input[placeholder*="品名"]').fill('精测');
    await p.locator('button:has-text("搜索")').first().click();
    await p.waitForTimeout(2000);
    let chk = await checkPageClean(p);
    console.log('15 clean?', JSON.stringify(chk));
    await shot(p, '15-warehouse-stock');

    // 点第一张验收图片看大图
    await p.locator('.el-table__row .el-image img').first().click();
    await p.waitForTimeout(2000);
    chk = await checkPageClean(p);
    console.log('15b clean?', JSON.stringify(chk));
    await p.screenshot({ path: `${FINAL}/15b-warehouse-photo-big.png` });
    console.log('[shot] 15b-warehouse-photo-big');
  } catch (e) {
    await fail(p, e, 'stage7');
  } finally {
    await b.close();
  }
})();
