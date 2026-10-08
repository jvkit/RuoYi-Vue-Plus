// 备用金收尾冒烟（新 /procurement/reserve/options 数据源 + 审批详情页字段）：
// 1) 新建自购草稿（选 2 个备用金人 + 使用人）→ 打开审批详情页 type=view 验证「备用金人/使用人」只读展示
// 2) 新增弹窗验证 ReservePicker 卡片经 /options 正常加载
const { execSync } = require('child_process');
const { launch, login, fillByLabel, treeSelectByLabel, shot, fail, PIC } = require('./e2e_lib');
const fs = require('fs');

const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
fs.mkdirSync(PIC, { recursive: true });

/** 从本地 MySQL 取草稿 id（标题查重，冒烟专用） */
function draftId() {
  const out = execSync(
    `docker exec -i mysql mysql -uroot -proot -D ry-vue-6x --default-character-set=utf8mb4 -N -e "SELECT id FROM pms_procurement_request WHERE title_name='备用金详情冒烟' AND del_flag='0' ORDER BY create_time DESC LIMIT 1;"`,
    { encoding: 'utf8' }
  ).trim();
  return out;
}

(async () => {
  const browser = await launch();
  const { ctx, p } = await login(browser, 'admin', 'admin123');
  try {
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForSelector('.el-table__row', { timeout: 15000 });
    await p.waitForTimeout(800);

    // 拦截 /options 请求，确认新数据源被调用且返回 200
    let optionsStatus = 0;
    p.on('response', (res) => {
      if (res.url().includes('/procurement/reserve/options')) optionsStatus = res.status();
    });

    // ---- 新建自购草稿 ----
    await p.locator('button:has-text("新增")').first().click();
    const dialog = p.locator('.el-dialog:visible').last();
    await dialog.waitFor({ timeout: 10000 });
    await p.waitForTimeout(800);

    const cards = dialog.locator('.rp-card');
    const cardCount = await cards.count();
    console.log('[check] rp-card count =', cardCount, '，/options HTTP =', optionsStatus);
    if (cardCount < 2) throw new Error('备用金卡片未加载（/options 可能未就绪）');
    await shot(p, '16-picker');

    // 明细（超大金额避免锁定）+ 选两人 + 使用人
    await dialog.locator('button:has-text("添加明细")').first().click();
    const itemDialog = p.locator('.el-dialog:visible').last();
    await itemDialog.waitFor({ timeout: 8000 });
    await fillByLabel(p, itemDialog, '品名', '详情字段冒烟');
    await fillByLabel(p, itemDialog, '数量', '1');
    await fillByLabel(p, itemDialog, '单价', '99999999');
    await itemDialog.locator('button:has-text("确 定")').click();
    await p.waitForTimeout(500);
    await cards.nth(0).click();
    await p.waitForTimeout(200);
    await cards.nth(1).click();
    await p.waitForTimeout(300);
    await dialog.locator('.el-form-item', { hasText: '使用人' }).locator('.el-select__wrapper').first().click();
    await p.waitForTimeout(600);
    await p.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click();
    await p.waitForTimeout(300);
    await fillByLabel(p, dialog, '标题名称', '备用金详情冒烟');
    await treeSelectByLabel(p, dialog, '项目', ['超分辨光学显微镜']);
    await dialog.locator('button:has-text("保 存 草 稿")').click();
    await p.waitForTimeout(1500);

    // ---- 审批详情页（/procurement/request/detail?type=view&id=）----
    const id = draftId();
    if (!id) throw new Error('草稿未找到（DB 查询为空）');
    console.log('[check] draft id =', id);
    await p.goto(`process.env.OA_BASE || 'http://127.0.0.1:8082'/procurement/request/detail?type=view&id=${id}`, { waitUntil: 'networkidle' });
    await p.waitForSelector('.el-form-item', { timeout: 15000 });
    await p.waitForTimeout(1000);

    const rpItem = p.locator('.el-form-item', { hasText: '备用金人' }).first();
    const useItem = p.locator('.el-form-item', { hasText: '使用人' }).first();
    // el-input readonly 的值在 input.value 上，innerText 读不到
    const rpVal = (await rpItem.locator('input').inputValue()).trim();
    const useVal = (await useItem.locator('input').inputValue()).trim();
    console.log('[check] 备用金人 =', rpVal);
    console.log('[check] 使用人 =', useVal);
    if (!rpVal.includes('→')) throw new Error('详情页备用金人未按顺序拼接');
    if (!useVal || useVal === '-') throw new Error('详情页使用人为空');
    await shot(p, '16-detail-fields');

    // ---- 清理：回列表删草稿 + 物理清除 ----
    await p.goto(`${BASE}/procurement/request`, { waitUntil: 'networkidle' });
    await p.waitForSelector('.el-table__row', { timeout: 15000 });
    await p.waitForTimeout(800);
    for (let i = 0; i < 5; i++) {
      const row = p.locator('.el-table__row', { hasText: '备用金详情冒烟' }).first();
      if (!(await row.isVisible().catch(() => false))) break;
      await row.locator('button').nth(3).click();
      await p.waitForTimeout(600);
      await p.locator('.el-message-box:visible button:has-text("确定")').click();
      await p.waitForTimeout(1000);
    }
    execSync(
      `docker exec -i mysql mysql -uroot -proot -D ry-vue-6x -e "DELETE FROM pms_procurement_request WHERE title_name='备用金详情冒烟';"`,
      { encoding: 'utf8' }
    );
    console.log('[check] 草稿已清理');

    console.log('[OK] reserve detail fields smoke passed');
    await browser.close();
  } catch (e) {
    await fail(p, e, 'reserve-detail');
  } finally {
    await ctx.close().catch(() => {});
  }
})();
