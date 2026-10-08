// 冒烟：拖拽改挂（dispatch DragEvent 版本）—— typec 发票(26442000000128349256) → 螺丝行挂载槽
const { launch, login } = require('./e2e_lib');
const BASE = (process.env.OA_BASE || 'http://127.0.0.1:8082').replace(/\/$/, '');
const PIC = '/home/jvkit/workspace/oa/temp/testing/pic';

(async () => {
  const b = await launch();
  const { ctx, p } = await login(b, 'admin', 'admin123');
  await p.goto(`${BASE}/procurement/invoice?requestId=2102645300967444481&openUpload=1`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(2500);

  const card = p.locator('[draggable="true"]', { hasText: '26442000000128349256' }).first();
  // 螺丝行的挂载槽（mount-slot 容器，无论是否有已挂载发票都存在）
  const slot = p.locator('.item-card', { hasText: '各类常用螺丝' }).locator('.mount-slot').first();
  await card.scrollIntoViewIfNeeded();
  await slot.scrollIntoViewIfNeeded();

  const dt = await p.evaluateHandle(() => new DataTransfer());
  await card.dispatchEvent('dragstart', { dataTransfer: dt });
  await slot.dispatchEvent('dragenter', { dataTransfer: dt });
  await slot.dispatchEvent('dragover', { dataTransfer: dt });
  await p.waitForTimeout(300); // 让高亮/吸附态渲染
  await p.screenshot({ path: `${PIC}/10-drag-highlight.png` });
  await slot.dispatchEvent('drop', { dataTransfer: dt });
  await card.dispatchEvent('dragend', { dataTransfer: dt });
  await p.waitForTimeout(1500);
  await p.screenshot({ path: `${PIC}/10-drag-result.png` });
  const tag = await p.locator('text=已挂载').first().innerText().catch(() => 'N/A');
  console.log('mounted tag:', tag);
  await b.close();
})().catch(e => { console.error('ERR', e.message); process.exit(1); });
