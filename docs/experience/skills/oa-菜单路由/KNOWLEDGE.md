# oa-菜单路由 — experience

> Shared cross-session lessons for `oa-菜单路由`.
> Auto-maintained by fireworks-skill-memory. Last updated: 2026-10-08

## Entries

- [2026-10] [RuoYi 动态路由 404] sys_menu 子菜单 path 必须写相对路径（如 `fund`），写成绝对路径 `/procurement/fund` 会出现「菜单显示正常但打开 404」—— 菜单存在≠路由可用；菜单改动必须落 `ruoyi-6x/script/sql/` 幂等脚本，修复类脚本按 `v<数字>_描述.sql` 命名确保 sort -V 排在创建脚本之后。  [HIT:1]
- [2026-10] [无权限直访落 404] 已登录用户访问「菜单未授权给自己」的 URL 时，RuoYi 前端 permission.ts 的 else 分支直接 return true，未匹配任何动态路由 → 落到 vue-router catch-all `/:pathMatch(.*)*` 显示 404 —— 404 在这里是"未授权"而非"页面缺失"。排障时先用 admin 对照：admin 路由表有该路径而普通用户没有，即可区分是菜单授权问题还是构建问题，避免误判 dist 旧。修复：permission.ts 在 roles 已加载且路由未注册时 return '/401'。  [HIT:1]
