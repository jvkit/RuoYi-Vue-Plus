// 重截 01/03/04/05（角色修复后 + tallShot）：01 首页 / 03 申请列表 / 04 新增弹窗(不提交) / 05 列表含已提交精测单
const { launch, login, shot, checkPageClean, fail, treeSelectByLabel, fillByLabel, selectAllowCreateByLabel } = require('./final_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const TITLE_NAME = '精测-显微镜载玻片采购';
const ITEMS = [
  { name: '精测-载玻片', spec: '75x25mm 帆船牌', unit: '盒', qty: 1, price: 100 },
  { name: '精测-盖玻片', spec: '20x20mm 100片装', unit: '盒', qty: 1, price: 100 }
];

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    // 01 首页
    await p.keyboard.press('Escape');
    await p.locator('.el-notification__closeBtn').first().click().catch(() => {});
    await p.waitForTimeout(400);
    await shot(p, '01-login');

    // 03 采购申请列表
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    let chk = await checkPageClean(p);
    console.log('03 clean?', JSON.stringify(chk));
    await shot(p, '03-request-list');

    // 04 新增弹窗（填完不提交）
    await p.locator('button:has-text("新增")').first().click();
    await p.waitForTimeout(1200);
    const dlg = p.locator('.el-dialog:visible').first();
    await dlg.locator('.el-radio', { hasText: '自购' }).first().click();
    await dlg.locator('input[placeholder="如：笔记本购买"]').fill(TITLE_NAME);
    await treeSelectByLabel(p, dlg, '项目', ['超分辨光学显微镜']);
    await dlg.locator('textarea[placeholder="请输入申请原因"]').fill('精测端到端测试：显微镜实验耗材采购');
    await dlg.locator('button:has-text("添加明细")').first().click();
    await p.waitForTimeout(1200);
    for (let i = 0; i < ITEMS.length; i++) {
      const it = ITEMS[i];
      const idlg = p.locator('.el-dialog:visible', { hasText: '添加明细' }).last();
      await idlg.waitFor({ state: 'visible' });
      await treeSelectByLabel(p, idlg, '分类', ['100材料']);
      await idlg.locator('input[placeholder="品名"]').fill(it.name);
      await idlg.locator('input[placeholder="规格型号"]').fill(it.spec);
      await selectAllowCreateByLabel(p, idlg, '单位', it.unit);
      await fillByLabel(p, idlg, '数量', it.qty);
      await fillByLabel(p, idlg, '单价', it.price);
      await selectAllowCreateByLabel(p, idlg, '物料用途', '科研');
      await idlg.locator('textarea[placeholder*="该明细的采购原因"]').fill('精测明细采购原因');
      if (i < ITEMS.length - 1) {
        await idlg.locator('button:has-text("再添加一个")').click();
        await p.waitForTimeout(1200);
      } else {
        await idlg.locator('button:has-text("确 定")').click();
        await p.waitForTimeout(800);
      }
    }
    await dlg.locator('.rp-card', { hasText: '冯忠俊' }).first().click();
    await p.waitForTimeout(600);
    await selectAllowCreateByLabel(p, dlg, '使用人', '冯忠俊');
    await dlg.locator('input[type=file]').first().setInputFiles('/home/jvkit/workspace/oa/test-invoices/good_invoice.png');
    await p.waitForTimeout(2500);
    chk = await checkPageClean(p);
    console.log('04 clean?', JSON.stringify(chk));
    await shot(p, '04-request-add');
    // 关闭弹窗不提交
    await p.keyboard.press('Escape');
    await p.locator('.el-dialog:visible .el-dialog__headerbtn').first().click().catch(() => {});
    await p.waitForTimeout(800);

    // 05 列表（含已提交的精测单）
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    const body = await p.locator('body').innerText();
    console.log('列表含精测标题:', body.includes(TITLE_NAME));
    chk = await checkPageClean(p);
    console.log('05 clean?', JSON.stringify(chk));
    await shot(p, '05-request-submitted');
  } catch (e) {
    await fail(p, e, 'reshoot1');
  } finally {
    await b.close();
  }
})();
