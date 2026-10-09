# oa-测试工具 — experience

> Shared cross-session lessons for `oa-测试工具`.
> Auto-maintained by fireworks-skill-memory. Last updated: 2026-10-09

## Entries

- [2026-10] [sed 批量改 JS 引号灾难] 用 sed 把 JS 里的字符串字面量 'http://host' 替换成含引号的表达式会生成嵌套引号语法错误，且每次 Bash 调用是独立 shell、cd 不跨调用生效导致"文件不存在"假错 —— 批量改造含引号的代码用 python 精确替换（定位坏串→replace→补定义），改完必须对每个文件 node --check 验证。  [HIT:1]
- [2026-10] [有头测试必须逐步暂停] 有头模式（未设 PW_AUTO）下用户要观察每步操作，脚本必须在关键步骤后暂停等用户按 Enter 继续，不能自动连跑到底——除非显式设 PW_AUTO=1（无头）。notice_test 曾自动跑完用户来不及看清弹窗。实现：有头时用 readline 暂停 + 提示「按 Enter 继续」。  [HIT:1]
- [2026-10] [通知弹窗需暂停观察] ElNotification 默认 3 秒消失，有头测试里要在弹窗出现后暂停（等用户看清再按 Enter），否则用户来不及确认。用 PW_STEP=1 或脚本内置暂停。  [HIT:1]
- [2026-10] [Playwright 对话框表单坑] el-dialog 里 force click 打开 el-tree-select 下拉后，option 的 span 有不可见副本（is-visible=false），要遍历找 isVisible=true 的元素再 click；操作列表格按钮无 title 属性，用 el-tooltip content 或按钮位置索引（查看0/修改1/提交2/删除3/导出4）定位。  [HIT:1]
