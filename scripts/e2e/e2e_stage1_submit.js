// E2E 阶段1：冯忠俊 提交采购申请（5明细，素材来自 8.0105淘宝件采购.xlsx）
const path = require('path');
const { launch, login, treeSelectByLabel, fillByLabel, selectAllowCreateByLabel, shot, fail } = require('./e2e_lib');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const ITEMS = [
  { name: '螺丝',   spec: '各类常用螺丝(M2 M2.5 M3 M3.5)', unit: '套', qty: 1,  price: 448.52 },
  { name: '螺母',   spec: '各类常用螺母(M8 M10 M11 M12)',   unit: '套', qty: 1,  price: 241.45 },
  { name: '加热带', spec: '直径8*300毫米 9Ω',               unit: '根', qty: 1,  price: 70.00 },
  { name: '512G内存卡', spec: 'TF高速红黑卡 闪迪',          unit: '个', qty: 1,  price: 415.00 },
  { name: 'GX12公头转XH2.54端子', spec: '4芯、200MM',        unit: '根', qty: 10, price: 39.90 },
];

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'fengzhongjun', '666666');
  try {
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2000);
    await shot(p, '01-1-request-list-empty');

    // 新增
    await p.locator('button:has-text("新增")').first().click();
    await p.waitForTimeout(1200);
    const dlg = p.locator('.el-dialog:visible').first();

    // 采购方式：自购
    await dlg.locator('.el-radio', { hasText: '自购' }).first().click();
    // 标题名称
    await dlg.locator('input[placeholder="如：笔记本购买"]').fill('0105淘宝件测试采购');
    // 项目（树选择）
    await treeSelectByLabel(p, dlg, '项目', ['全画幅结构光超分辨显微镜系统']);
    // 采购种类默认已是科研类，无需选择
    // 申请原因
    await dlg.locator('textarea[placeholder="请输入申请原因"]').fill('研发仪器所需零件');
    await shot(p, '01-2-request-form-main');

    // 逐条添加明细
    // 打开明细弹窗（"再添加一个"后弹窗保持打开并重置，无需重复点添加明细）
    await dlg.locator('button:has-text("添加明细")').first().click();
    await p.waitForTimeout(1200);

    for (let i = 0; i < ITEMS.length; i++) {
      const it = ITEMS[i];
      const idlg = p.locator('.el-dialog:visible', { hasText: '添加明细' }).last();
      await idlg.waitFor({ state: 'visible' });
      console.log('item', i, it.name, '：分类...');
      // 分类：100材料 -> 112科研类低值易耗品（默认100材料，需展开换选）
      await treeSelectByLabel(p, idlg, '分类', ['100材料', '112科研类低值易耗品']);
      // 品名 / 规格
      await idlg.locator('input[placeholder="品名"]').fill(it.name);
      await idlg.locator('input[placeholder="规格型号"]').fill(it.spec);
      // 单位（allow-create，默认"个"）
      await selectAllowCreateByLabel(p, idlg, '单位', it.unit);
      console.log('item', i, '：数量单价...');
      // 数量 / 单价（普通输入框，按 label 定位）
      await fillByLabel(p, idlg, '数量', it.qty);
      await fillByLabel(p, idlg, '单价', it.price);
      console.log('item', i, '：物料用途...');
      // 物料用途
      await selectAllowCreateByLabel(p, idlg, '物料用途', '研发');
      console.log('item', i, '：采购原因...');
      // 采购原因
      await idlg.locator('textarea[placeholder*="该明细的采购原因"]').fill('研发仪器所需零件');
      if (i < ITEMS.length - 1) {
        await idlg.locator('button:has-text("再添加一个")').click();
        await p.waitForTimeout(1200); // 弹窗重置后继续填下一条
      } else {
        await idlg.locator('button:has-text("确 定")').click();
        await p.waitForTimeout(800);
      }
    }
    await shot(p, '01-3-request-items-filled');

    // 自购必须上传付款截图（素材无截图，用测试图 mock）
    await dlg.locator('input[type=file]').first().setInputFiles('/home/jvkit/workspace/oa/test-invoices/good_invoice.png');
    await p.waitForTimeout(2500);
    await shot(p, '01-3b-payment-screenshot');

    // 提交申请
    await dlg.locator('button:has-text("提 交 申 请")').click();
    await p.waitForTimeout(1500);
    // 可能的确认框
    const confirmBtn = p.locator('.el-message-box:visible button:has-text("确 定"), .el-message-box:visible button:has-text("确定")');
    if (await confirmBtn.count()) { await confirmBtn.first().click(); await p.waitForTimeout(2500); }
    await shot(p, '01-4-request-submitted');

    // 回列表验证状态
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(2500);
    const body = await p.locator('body').innerText();
    console.log('列表含新申请:', body.includes('0105淘宝件测试采购'));
    console.log('BODY片段:', body.replace(/\n+/g, ' | ').slice(0, 260));
    await shot(p, '01-5-request-list-after');
  } catch (e) {
    await fail(p, e, 'stage1');
  } finally {
    await b.close();
  }
})();
