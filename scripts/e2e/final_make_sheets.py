#!/usr/bin/env python
"""把 final/ 截图按文件名排序拼成 3x3 contact sheet，每张缩略图宽 500，下方标注文件名。"""
import os
from PIL import Image, ImageDraw, ImageFont

FINAL = '/home/jvkit/workspace/oa/temp/testing/output/final'
THUMB_W = 500
COLS, ROWS = 3, 3
CAPTION_H = 30
GAP = 12
BG = (245, 245, 245)

def natural_key(name):
    # 01 < 02 < 06 < 06b < 07 ... 按 (数字前缀, 名字) 排序
    num = ''
    for ch in name:
        if ch.isdigit():
            num += ch
        else:
            break
    return (int(num) if num else 0, name)

files = sorted([f for f in os.listdir(FINAL) if f.endswith('.png') and not f.startswith('sheet-')], key=natural_key)
print(f'{len(files)} 张截图')

# 先看最大缩略图高度
thumbs = []
max_h = 0
for f in files:
    im = Image.open(os.path.join(FINAL, f)).convert('RGB')
    r = THUMB_W / im.width
    im = im.resize((THUMB_W, int(im.height * r)))
    thumbs.append((f, im))
    max_h = max(max_h, im.height)

cell_w = THUMB_W
cell_h = max_h + CAPTION_H
per_page = COLS * ROWS

try:
    font = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 18)
except Exception:
    font = ImageFont.load_default()

for page in range((len(thumbs) + per_page - 1) // per_page):
    sheet = Image.new('RGB', (COLS * cell_w + (COLS + 1) * GAP, ROWS * cell_h + (ROWS + 1) * GAP), BG)
    draw = ImageDraw.Draw(sheet)
    for i in range(per_page):
        idx = page * per_page + i
        if idx >= len(thumbs):
            break
        name, im = thumbs[idx]
        r, c = divmod(i, COLS)
        x = GAP + c * (cell_w + GAP)
        y = GAP + r * (cell_h + GAP)
        sheet.paste(im, (x, y))
        draw.text((x + 4, y + im.height + 4), name, fill=(20, 20, 20), font=font)
    out = os.path.join(FINAL, f'sheet-{page + 1}.png')
    sheet.save(out)
    print('saved', out, sheet.size)
