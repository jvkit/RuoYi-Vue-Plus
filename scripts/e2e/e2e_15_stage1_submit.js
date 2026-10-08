// E2E-15 阶段1：冯忠俊提交自购采购申请（合计 11500.00，备用金人按顺序选 冯忠俊→彭赛威）
// 预期拆账：冯忠俊可用 8426.03 全扣 + 彭赛威兜尾差 3073.97
const { launch, login, treeSelectByLabel, fillByLabel, selectAllowCreateByLabel, shot, fail, PIC } = require('./e2e_lib');
const fs = require('fs');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
fs.mkdirSync(PIC, { recursive: true });

const TITLE_NAME = '拆账联测-多备用金人拆分';
const ITEMS = [
  { name: '高功率固态激光器', spec: '500mW 532nm', unit: '台', qty: 1, price: 8000.0 },
  { name: '科研级相机模组', spec: '高灵敏度CMOS', unit: '台', qty: 1, price: 3500.0 }
];

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2000);
    await shot(p, '15-e2e-01-request-list');

    // 新增
    await p.locator('button:has-text("新增")').first().click();
    await p.waitForTimeout(1200);
    const dlg = p.locator('.el-dialog:visible').first();

    // 采购方式：自购
    await dlg.locator('.el-radio', { hasText: '自购' }).first().click();
    await dlg.locator('input[placeholder="如：笔记本购买"]').fill(TITLE_NAME);
    // 项目（叶子：超分辨光学显微镜，负责人=李迪）
    await treeSelectByLabel(p, dlg, '项目', ['超分辨光学显微镜']);
    await dlg.locator('textarea[placeholder="请输入申请原因"]').fill('拆账联测：验证多备用金人按顺序拆分资金流水');
    await p.waitForTimeout(400);
    // 项目负责人应自动带出李迪
    const leaderText = await dlg.locator('.el-form-item', { hasText: '项目负责人' }).locator('input').inputValue().catch(() => '');
    console.log('[check] 项目负责人带出 =', leaderText);

    // 备用金卡片加载完成
    await dlg.locator('.rp-card').first().waitFor({ timeout: 10000 });
    const cardNames = await dlg.locator('.rp-card .rp-name').allInnerTexts();
    console.log('[check] 备用金卡片 =', JSON.stringify(cardNames));

    // 按顺序点选：冯忠俊（1号）→ 彭赛威（2号）（卡片顺序按 personId 排序，必须按名字点）
    await dlg.locator('.rp-card', { hasText: '冯忠俊' }).first().click();
    await p.waitForTimeout(400);
    await dlg.locator('.rp-card', { hasText: '彭赛威' }).first().click();
    await p.waitForTimeout(500);

    // 读取两张卡的序号验证顺序
    const orderOf = async (name) => {
      const badge = dlg.locator('.rp-card', { hasText: name }).first().locator('.rp-order');
      return (await badge.count()) ? (await badge.innerText()).trim() : '';
    };
    const o1 = await orderOf('冯忠俊');
    const o2 = await orderOf('彭赛威');
    console.log('[check] 冯忠俊序号 =', o1, '，彭赛威序号 =', o2);
    if (o1 !== '1' || o2 !== '2') throw new Error(`备用金人顺序不对: 冯=${o1} 彭=${o2}`);
    const tip = await dlg.locator('.rp-tip').innerText();
    console.log('[check] rp-tip =', tip);

    // 使用人：随便选第一个
    await dlg.locator('.el-form-item', { hasText: '使用人' }).locator('.el-select__wrapper').first().click();
    await p.waitForTimeout(700);
    await p.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click();
    await p.waitForTimeout(300);

    // 明细
    await dlg.locator('button:has-text("添加明细")').first().click();
    await p.waitForTimeout(1200);
    for (let i = 0; i < ITEMS.length; i++) {
      const it = ITEMS[i];
      const idlg = p.locator('.el-dialog:visible', { hasText: '添加明细' }).last();
      await idlg.waitFor({ state: 'visible' });
      await treeSelectByLabel(p, idlg, '分类', ['100材料', '112科研类低值易耗品']);
      await idlg.locator('input[placeholder="品名"]').fill(it.name);
      await idlg.locator('input[placeholder="规格型号"]').fill(it.spec);
      await selectAllowCreateByLabel(p, idlg, '单位', it.unit);
      await fillByLabel(p, idlg, '数量', it.qty);
      await fillByLabel(p, idlg, '单价', it.price);
      await selectAllowCreateByLabel(p, idlg, '物料用途', '研发');
      await idlg.locator('textarea[placeholder*="该明细的采购原因"]').fill('拆账联测明细');
      if (i < ITEMS.length - 1) {
        await idlg.locator('button:has-text("再添加一个")').click();
        await p.waitForTimeout(1200);
      } else {
        await idlg.locator('button:has-text("确 定")').click();
        await p.waitForTimeout(800);
      }
    }
    const totalText = await dlg.locator('.el-card', { hasText: '采购明细' }).locator('.mt-2').innerText().catch(() => '');
    console.log('[check] 合计 =', totalText);
    await shot(p, '15-e2e-02-request-form-filled');

    // 付款截图（自购必填）
    await dlg.locator('input[type=file]').first().setInputFiles('/home/jvkit/workspace/oa/test-invoices/good_invoice.png');
    await p.waitForTimeout(2500);

    // 提交申请
    await dlg.locator('button:has-text("提 交 申 请")').click();
    await p.waitForTimeout(1500);
    const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
    if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2500); }
    await shot(p, '15-e2e-03-request-submitted');

    // 回列表验证
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    const body = await p.locator('body').innerText();
    console.log('[check] 列表含新申请:', body.includes(TITLE_NAME));
    await shot(p, '15-e2e-04-request-list-after');
    console.log('[OK] stage1 submit done');
    await b.close();
  } catch (e) {
    await fail(p, e, '15-stage1');
  } finally {
    await ctx.close().catch(() => {});
  }
})();
