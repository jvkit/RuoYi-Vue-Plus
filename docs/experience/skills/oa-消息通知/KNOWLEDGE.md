# oa-消息通知 — experience

> Shared cross-session lessons for `oa-消息通知`.
> Auto-maintained by fireworks-skill-memory. Last updated: 2026-10-09

## Entries

- [2026-10] [消息盒子三层数据源] RuoYi 6x 顶栏消息盒子分「系统/通知/工作」：系统=代码推送(登录欢迎)、通知=sys_notice 公告表(管理员发)、工作=workflow 消息(sys_message 表 source=workflow)。工作流审批/提交消息由 FlwCommonServiceImpl.sendMessage 写 sys_message(source=workflow)，前端 push.ts initMessageBox 拉 /resource/message/box 按 category 分流到三个页签。排查消息问题时先 curl /resource/message/box 看分类数据，再查 sys_message.source。  [HIT:1]
- [2026-10] [消息盒子本地正常但用户报零] 先确认用户是否登录的是同一环境/账号；接口层 curl 认证后直接调 /resource/message/box 看分类计数，比浏览器调试快。  [HIT:1]
