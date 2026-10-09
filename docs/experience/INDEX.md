# 经验库索引（项目级 · oa）

动手前先扫"触发条件"列，命中就读对应文件；没命中就直接干活，不要全量加载。
CLI：`AI_MEMORY_HOME=<项目根>/docs/experience python3 ~/.agents/lib/fireworks-skill-memory/cli/skill_memory.py ...`

| 范围 | 触发条件 | 文件 |
|---|---|---|
| oa-部署 | 服务器部署、apply-sql.sh/deploy.sh、写带 set -e 的 bash、pkill 批量杀进程 | skills/oa-部署/KNOWLEDGE.md |
| oa-菜单路由 | sys_menu 菜单/权限改动、页面「菜单在但打开 404/无权访问」排查 | skills/oa-菜单路由/KNOWLEDGE.md |
| oa-前端样式 | plus-ui-6x 布局/侧栏/主题配色调整、外部注入 CSS 不生效 | skills/oa-前端样式/KNOWLEDGE.md |
| oa-测试工具 | scripts/e2e 导演脚本、Playwright、sed/python 批量改代码 | skills/oa-测试工具/KNOWLEDGE.md |
| oa-消息通知 | 消息盒子/工作流通知不显示、sys_message/sys_notice 排查、推送链路 | skills/oa-消息通知/KNOWLEDGE.md |
