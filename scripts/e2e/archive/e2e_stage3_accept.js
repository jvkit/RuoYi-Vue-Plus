// E2E 阶段3：彭赛威 创建验收单（5 明细实物图，素材来自 8.0105淘宝件验收图）
const { launch, login, selectOptionByLabel, shot, fail } = require('./e2e_lib');

const BASE = '/home/jvkit/workspace/oa/temp/testing/采购素材(1)/采购素材/8.0105淘宝件验收图';
const PHOTOS = {
  '螺丝': `${BASE}/螺丝.jpg`,
  '螺母': `${BASE}/螺帽.jpg`,
  '加热带': `${BASE}/接热带.jpg`,
  '512G内存卡': `${BASE}/512G内存卡.jpg`,
  'GX12公头转XH2.54端子': '/home/jvkit/workspace/oa/temp/testing/GX12_compressed.jpg',
};

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'pengsaiwei', '666666');
  try {
    await p.goto(`${BASE}/procurement/acceptance`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2000);
    await shot(p, '03-1-acceptance-list');

    await p.locator('button:has-text("新增")').first().click();
    await p.waitForTimeout(1500);
    const dlg = p.locator('.el-dialog:visible').first();
    // 关联采购申请
    await selectOptionByLabel(p, dlg, '关联采购申请', '0105淘宝件测试采购');
    await p.waitForTimeout(2000);
    await shot(p, '03-2-acceptance-items-loaded');

    // 每行上传实物图
    const rows = dlg.locator('.el-table__row');
    const n = await rows.count();
    console.log('明细行数:', n);
    for (let i = 0; i < n; i++) {
      const row = rows.nth(i);
      const name = (await row.locator('td').first().innerText()).trim();
      const file = PHOTOS[name];
      console.log(`行${i}: 「${name}」 -> ${file ? file.split('/').pop() : '无匹配!'}`);
      if (!file) continue;
      await row.locator('input[type=file]').setInputFiles(file);
      // 等缩略图出来才算传完
      await row.locator('.el-upload-list img, .el-upload-list__item, img').first().waitFor({ state: 'visible', timeout: 20000 });
      await p.waitForTimeout(800);
    }
    // 验收日期（必填）
    await dlg.locator('input[placeholder="选择验收日期"]').fill('2026-09-23');
    await p.keyboard.press('Enter');
    await p.waitForTimeout(600);
    await shot(p, '03-3-acceptance-photos');

    // 提交
    await dlg.locator('button:has-text("提 交")').click();
    await p.waitForTimeout(1500);
    const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
    if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2500); }
    await shot(p, '03-4-acceptance-submitted');
    console.log('验收单已提交');
  } catch (e) {
    await fail(p, e, 'stage3');
  } finally {
    await b.close();
  }
})();
