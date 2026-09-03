#!/usr/bin/env python3
# 生成《OA 协同办公系统 用户手册》Word 版
# 用法: .venv/bin/python build_manual.py
# 截图来源: ../../../temp/manual-shots/（Playwright 实拍，已目检）
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
import os

SHOTS = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'temp', 'manual-shots'))
OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'docs', 'OA协同办公系统-用户手册.docx'))

doc = Document()

# 全局中文字体
style = doc.styles['Normal']
style.font.name = 'Calibri'
style.font.size = Pt(11)
style.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')

def set_cn(run, size=None, bold=None, color=None):
    run.font.name = 'Calibri'
    run.element.rPr.rFonts.set(qn('w:eastAsia'), '微软雅黑')
    if size: run.font.size = Pt(size)
    if bold is not None: run.font.bold = bold
    if color: run.font.color.rgb = color

def h1(text):
    p = doc.add_heading(level=1)
    r = p.add_run(text); set_cn(r, 18, True, RGBColor(0x1F, 0x4E, 0x79))

def h2(text):
    p = doc.add_heading(level=2)
    r = p.add_run(text); set_cn(r, 14, True, RGBColor(0x2E, 0x74, 0xB5))

def para(text, bold=False, size=11):
    p = doc.add_paragraph()
    r = p.add_run(text); set_cn(r, size, bold)
    return p

def bullet(text):
    p = doc.add_paragraph(style='List Bullet')
    r = p.add_run(text); set_cn(r, 11)

def numbered(text):
    p = doc.add_paragraph(style='List Number')
    r = p.add_run(text); set_cn(r, 11)

def note(text):
    p = doc.add_paragraph()
    r = p.add_run('提示：' + text); set_cn(r, 10.5, False, RGBColor(0xC0, 0x50, 0x4D))

def shot(name, caption, width_cm=15.5):
    path = os.path.join(SHOTS, name + '.png')
    if not os.path.exists(path):
        print('MISSING SHOT:', name); return
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.add_run().add_picture(path, width=Cm(width_cm))
    c = doc.add_paragraph(); c.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = c.add_run(caption); set_cn(r, 9.5, False, RGBColor(0x7F, 0x7F, 0x7F))

def table(headers, rows, widths=None):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = 'Light Grid Accent 1'
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, h in enumerate(headers):
        r = t.rows[0].cells[i].paragraphs[0].add_run(h); set_cn(r, 10.5, True)
    for row in rows:
        cells = t.add_row().cells
        for i, v in enumerate(row):
            r = cells[i].paragraphs[0].add_run(str(v)); set_cn(r, 10.5)
    doc.add_paragraph()

# ============ 封面 ============
for _ in range(6): doc.add_paragraph()
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('OA 协同办公系统'); set_cn(r, 36, True, RGBColor(0x1F, 0x4E, 0x79))
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('用 户 手 册'); set_cn(r, 28, True)
doc.add_paragraph()
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('（内部使用）'); set_cn(r, 14)
for _ in range(8): doc.add_paragraph()
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('系统地址：http://172.16.16.110/oa/\n更新日期：2026-09-03    系统版本：OA 6x（RuoYi-Vue-Plus 6.0.0）'); set_cn(r, 12)
doc.add_page_break()

# ============ 一、访问方式 ============
h1('一、访问方式')
h2('1.1 电脑端')
para('打开浏览器（推荐 Chrome / Edge），访问：')
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('http://172.16.16.110/oa/'); set_cn(r, 13, True)
para('打开后进入登录页面，输入用户名和密码即可登录。')
shot('01-login', '图 1-1 系统登录页')
h2('1.2 手机端')
para('手机与办公网络（172.16.16.x 网段）保持连通，浏览器访问同一地址 http://172.16.16.110/oa/ 即可；也可在电脑端点击左侧菜单「手机端专属」→「移动验收」进入手机专用验收页面（详见第十二章）。')

# ============ 二、账号与角色 ============
doc.add_page_break()
h1('二、账号与密码')
h2('2.1 账号一览')
table(['姓名', '用户名', '密码', '角色'],
[
    ['admin（管理员）', 'admin', 'admin123', '超级管理员，拥有全部权限'],
    ['王建龙', 'wangjianlong', '666666', 'CEO、超级管理员'],
    ['李迪', 'lidi', '666666', '最高决策人、超级管理员'],
    ['裴天姿', 'peitianzi', '666666', '仓库管理员、验收对接人'],
    ['彭赛威', 'pengsaiwei', '666666', '普通用户'],
    ['张岩', 'zhangyan', '666666', '普通用户（部分项目任项目负责人）'],
    ['李炳晨', 'libingchen', '666666', '普通用户'],
    ['蒋泽鹏', 'jiangzepeng', '666666', '普通用户'],
    ['冯忠俊', 'fengzhongjun', '666666', '普通用户'],
    ['陈俊荣', 'chenjunrong', '666666', '普通用户'],
    ['何鑫', 'hexin', '666666', '普通用户'],
    ['贺鑫雨', 'hexinyu', '666666', '普通用户'],
])
note('所有非管理员账号初始密码均为 666666，首次登录后建议立即修改密码（修改方法见第四章）。')
h2('2.2 角色说明')
table(['角色', '人员', '权限说明'],
[
    ['普通用户', '全体同事', '提交采购申请、采购验收、领用申请；查看仓库库存；无库存操作权限'],
    ['项目负责人', '按项目指定', '审批本项目相关的采购申请、采购验收、领用申请（自动按项目归属确定）'],
    ['CEO', '王建龙', '审批全部采购申请；审批采购验收的最后一步'],
    ['最高决策人', '李迪', '审批总金额 ≥ 1000 元的采购申请'],
    ['仓库管理员', '裴天姿', '审批领用申请；管理仓库库存'],
    ['超级管理员', 'admin、王建龙、李迪', '系统全部权限，可代批任何流程'],
])
note('审批人为「角色」而非具体某人：同一角色下的任意一人审批通过即视为该环节通过。')

# ============ 三、登录与首页 ============
doc.add_page_break()
h1('三、登录与首页')
numbered('浏览器打开 http://172.16.16.110/oa/ ；')
numbered('输入用户名、密码，点击「登 录」；')
numbered('登录后进入首页，点击左侧菜单开始使用。')
para('普通用户登录后可见的菜单为：首页、工作流（我的任务）、PRIME AI、采购管理（采购申请 / 采购验收 / 仓库库存 / 领用申请）、手机端专属。项目管理、发票台账、供应商等管理功能仅管理员可见。')
shot('02-home', '图 3-1 首页（普通用户视角）')
para('首页中部提供常用快捷入口：「进入采购申请」「打开 PRIME AI」，可直接跳转。')

# ============ 四、个人中心与修改密码 ============
doc.add_page_break()
h1('四、个人中心与修改密码')
h2('4.1 进入个人中心')
numbered('点击页面右上角的头像，弹出下拉菜单；')
numbered('点击「个人中心」。')
shot('03-avatar-menu', '图 4-1 点击右上角头像，选择「个人中心」')
h2('4.2 修改密码')
numbered('在个人中心页面点击「修改密码」标签页；')
numbered('依次填写旧密码、新密码、确认密码（新密码需与确认密码一致）；')
numbered('点击「保存」即完成修改，下次登录请使用新密码。')
shot('04-profile', '图 4-2 个人中心（基本资料页）')
shot('05-change-pwd', '图 4-3 修改密码')
note('修改密码后如忘记新密码，请联系管理员（admin）重置。')

# ============ 五、采购申请 ============
doc.add_page_break()
h1('五、采购申请')
para('采购申请是整个采购流程的起点：填写申请 → 审批 → 验收 → 入库 → 领用。')
h2('5.1 进入页面')
para('左侧菜单 → 采购管理 → 采购申请。列表中可查看所有申请及其状态、当前审批人。')
shot('06-request-list', '图 5-1 采购申请列表')
para('列表操作按钮（每行右侧）依次为：详情、修改、提交、删除、导出申请表；顶栏还有新增、导出。')
h2('5.2 新建采购申请')
numbered('点击「新增」，弹出添加采购申请窗口；')
numbered('选择采购方式（自购 / 对公），填写标题名称（如「笔记本购买」）；')
numbered('点击「项目」选择归属项目（树形展开，支持多级项目），项目负责人自动带出；')
numbered('选择采购种类（科研类 / 非科研类）；')
numbered('自购需上传实际付款截图（不超过 5MB 的 png/jpg）；')
numbered('填写申请原因，点击「添加明细」逐条录入商品。')
shot('07-request-new', '图 5-2 新增采购申请')
h2('5.3 添加采购明细')
para('在「添加明细」弹窗中填写：分类（树形选择，默认 100 材料）、品名、规格、单位、数量、单价、商品链接、物料用途（默认「研发」，可改选或自填）、采购原因、备注。点击「确定」加入明细，可继续「再添加一个」。系统按单价 × 数量自动计算合计金额。')
shot('07b-request-item', '图 5-3 添加明细弹窗')
numbered('明细填写完成后，核对合计金额与项目可用资金；')
numbered('点击「保存草稿」暂存（可稍后修改），或点击「提交申请」正式进入审批流程。')
note('提交后申请不可再修改；如需调整，须等待审批驳回或联系管理员。')
h2('5.4 审批流程')
para('采购申请的审批链如下：')
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('提交 → 项目负责人审批 → CEO 审批 → 按金额自动分支\n总金额 < 1000 元：流程结束\n总金额 ≥ 1000 元：最高决策人审批 → 流程结束'); set_cn(r, 11.5, True)
shot('09b-flow-definition', '图 5-4 采购申请审批流程图')
para('任何一级都可选择「驳回」，被驳回的申请可修改后重新提交。')
h2('5.5 查看详情与流程进度')
para('在列表中点击行首的「详情」图标，可查看申请完整内容与采购明细；')
para('在详情页点击「流程进度」，可查看流程图与各级审批意见。')
shot('08-request-detail', '图 5-5 采购申请详情页')
h2('5.6 导出申请表')
para('每行操作列的「导出申请表」按钮可按该申请生成 Excel 采购清单（文件名自动使用申请标题）；顶栏「导出」可导出当前查询结果列表。')

# ============ 六、审批办理 ============
doc.add_page_break()
h1('六、我的任务与审批办理')
h2('6.1 我的任务')
para('左侧菜单 → 我的任务，包含四个子菜单：我发起的、我的待办、我的已办、我的抄送。')
bullet('我发起的：我提交的所有流程单据；')
bullet('我的待办：等待我审批的任务；')
bullet('我的已办：我已审批过的任务。')
shot('16-my-tasks', '图 6-1 我的待办')
h2('6.2 办理审批')
numbered('在「我的待办」中找到待办任务，点击「办理」；')
numbered('系统打开对应单据的审批详情页，可查看单据内容与流程进度；')
numbered('点击「审批」，选择通过或驳回；驳回时请注明原因；')
numbered('确认后流程自动流转到下一审批人。')
shot('18-approval', '图 6-2 采购申请审批详情页（点击「审批」办理）')
note('普通用户可以查看所有流程图（只读），但无法编辑或删除流程定义。')

# ============ 七、采购验收 ============
doc.add_page_break()
h1('七、采购验收')
para('采购申请审批通过后，货物到达即可发起验收；验收通过后商品自动入库。')
h2('7.1 发起验收')
numbered('左侧菜单 → 采购管理 → 采购验收，点击「新增」；')
numbered('在「关联采购申请」中选择已审批通过的采购申请（未验收的申请），项目与商品明细自动带出；')
numbered('填写验收日期，逐条上传实物图片与发票附件（均必填，不超过 5MB）；')
numbered('填写每条明细的发票金额，系统自动核对：不超过申请单价显示「通过」，超出显示「冲红」；')
numbered('确认无误后点击「提交」，也可「保存草稿」稍后继续。')
shot('11-acceptance-new', '图 7-1 添加采购验收')
shot('11b-acceptance-edit', '图 7-2 验收明细（实物图片 / 发票附件 / 金额核对）')
h2('7.2 验收审批流程')
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('验收发起人提交 → 采购申请人确认 → 项目负责人审批 → CEO 审批 → 结束'); set_cn(r, 11.5, True)
para('注意：发起人不一定等于采购申请人，但流程第二步固定由采购申请人确认。流程图中最后一步节点名显示为「团队上级审批」，实际审批角色为 CEO。')
h2('7.3 验收状态')
table(['验收标志', '含义'],
[
    ['未验收', '采购申请审批通过，尚未发起验收'],
    ['验收中', '已发起验收，正在审批'],
    ['已完成验收', '验收流程结束，商品已入库，可在仓库库存查看'],
])
para('每个采购申请只能发起一次验收；验收被驳回后可修改重新提交。')

# ============ 八、AI 发票识别匹配 ============
doc.add_page_break()
h1('八、AI 发票识别匹配')
para('在采购验收窗口中，点击「AI 识别发票」可一次性上传多张发票 PDF，由 AI 自动完成识别与匹配：')
numbered('AI 识别每张发票的票面字段（号码、开票日期、销售方、购买方、不含税金额、商品名等）；')
numbered('按商品名与金额自动匹配到对应验收明细，自动回填「发票金额」并上传发票附件；')
numbered('冲红发票自动标记「冲红」提示；与本订单不相干的发票不会自动上传；')
numbered('识别结束后给出匹配报告：成功匹配多少张、哪些商品缺少发票、哪些发票无法对应；')
numbered('缺少发票的商品可再次点击「AI 识别发票」补充上传（支持多轮）。')
shot('12-ai-invoice', '图 8-1 AI 批量识别发票')
note('目前仅识别发票 PDF 的第一页；金额口径为不含税金额；若发票商品名过长可能与订单商品名对不上，此时 AI 不会自动回填，请人工核对后手动填写。')

# ============ 九、仓库库存 ============
doc.add_page_break()
h1('九、仓库库存')
para('左侧菜单 → 采购管理 → 仓库库存。所有同事均可查看库存（品名、规格、单位、可用数量、所属项目、入库日期、验收图片），普通用户为只读；库存的增删改由仓库管理员操作。')
shot('13-warehouse', '图 9-1 仓库库存（点击验收图片可放大查看）')

# ============ 十、领用申请 ============
doc.add_page_break()
h1('十、领用申请')
h2('10.1 发起领用')
numbered('左侧菜单 → 采购管理 → 领用申请，点击「新增」；')
numbered('选择库存物料，品名、规格、可用数量自动带出；')
numbered('填写领用数量（不得超过可用数量）与用途；')
numbered('点击「提交」进入审批，可「保存」草稿。')
shot('14-issue-list', '图 10-1 领用申请列表')
shot('15-issue-new', '图 10-2 添加领用申请')
h2('10.2 领用审批流程')
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('提交申请 → 项目负责人审批 → 仓库管理员审批 → 结束'); set_cn(r, 11.5, True)
para('审批通过后仓库管理员出库，库存相应扣减。')

# ============ 十一、PRIME AI ============
doc.add_page_break()
h1('十一、PRIME AI')
para('PRIME AI 是公司内部的 AI 助手平台（智能对话、文档处理等）。点击左侧菜单「PRIME AI」（或首页「打开 PRIME AI」按钮）即可跳转：')
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('http://172.16.16.110:3305'); set_cn(r, 12, True)
shot('17-primeai', '图 11-1 PRIME AI 登录页')
note('PRIME AI 使用独立的账号体系，与 OA 账号不通用。首次使用请找管理员开通账号。')

# ============ 十二、手机端 ============
h1('十二、手机端专属')
para('手机浏览器访问系统后，点击左侧菜单「手机端专属」→「移动验收」，或直接在手机浏览器输入：')
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run('http://172.16.16.110/oa/mobile/acceptance'); set_cn(r, 12, True)
para('移动验收页面针对手机操作优化：可直接拍照上传验收图片、选择发票文件，随时随地完成验收。')
shot('19-mobile', '图 12-1 移动验收（手机端）', width_cm=7)

# ============ 十三、常见问题 ============
doc.add_page_break()
h1('十三、常见问题')
faq = [
    ('Q1：提示「没有访问权限，请联系管理员授权」怎么办？',
     '请联系管理员（admin）确认账号绑定的角色。普通用户应绑定「普通用户」角色；项目负责人、CEO、最高决策人、仓库管理员等为附加角色，可与普通用户角色同时绑定。'),
    ('Q2：提交采购申请时提示「项目资金不足」？',
     '所选项目剩余预算不足以覆盖本次合计金额，请更换项目或联系管理员调整项目预算。'),
    ('Q3：采购验收时找不到要验收的采购申请？',
     '只有审批通过且「未验收」的采购申请才会出现在关联下拉框中；每个申请只能验收一次。'),
    ('Q4：AI 识别发票后金额没有自动回填？',
     'AI 按商品名和金额匹配。若发票商品名与订单明细差异过大则不会自动回填（避免填错），请人工核对后手动填写发票金额并上传附件。'),
    ('Q5：流程被驳回后怎么办？',
     '在对应列表找到该单据，点击「详情」查看驳回原因，「修改」后重新提交即可，原审批流程重新流转。'),
    ('Q6：图片或 PDF 打不开、显示不出？',
     '请确认电脑/手机与服务器在同一内网（172.16.16.x）；仍不行请联系管理员检查文件服务。'),
    ('Q7：密码忘了怎么办？',
     '联系管理员（admin）重置密码。'),
    ('Q8：采购申请保存草稿后在哪里继续编辑？',
     '采购申请列表中状态为「草稿」的申请，点击行内「修改」按钮即可继续编辑并提交。'),
]
for q, a in faq:
    para(q, bold=True)
    para(a)

# ============ 附录 ============
doc.add_page_break()
h1('附录：账号速查表')
table(['用户名', '密码', '姓名', '角色'],
[
    ['admin', 'admin123', '管理员', '超级管理员'],
    ['wangjianlong', '666666', '王建龙', 'CEO / 超级管理员'],
    ['lidi', '666666', '李迪', '最高决策人 / 超级管理员'],
    ['peitianzi', '666666', '裴天姿', '仓库管理员'],
    ['pengsaiwei', '666666', '彭赛威', '普通用户'],
    ['zhangyan', '666666', '张岩', '普通用户'],
    ['libingchen', '666666', '李炳晨', '普通用户'],
    ['jiangzepeng', '666666', '蒋泽鹏', '普通用户'],
    ['fengzhongjun', '666666', '冯忠俊', '普通用户'],
    ['chenjunrong', '666666', '陈俊荣', '普通用户'],
    ['hexin', '666666', '何鑫', '普通用户'],
    ['hexinyu', '666666', '贺鑫雨', '普通用户'],
])
para('')
para('技术支持：系统管理员（admin / admin123）。本手册截图基于 2026-09-03 系统实际界面截取。', size=10)

doc.save(OUT)
print('SAVED:', OUT)
