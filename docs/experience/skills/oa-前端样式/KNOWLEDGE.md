# oa-前端样式 — experience

> Shared cross-session lessons for `oa-前端样式`.
> Auto-maintained by fireworks-skill-memory. Last updated: 2026-10-02

## Entries

- [2026-10] [plus-ui-6x 侧栏样式覆盖] 侧栏菜单样式定义在 `#app .sidebar-container` 高优先级作用域（_menu.scss），且 `.sidebar-shell` 背景是组件内 v-bind 内联 `!important` —— 外部注入 CSS 覆盖必须带 `#app` 前缀+`!important`；改底色直接改 `Sidebar/index.vue` 的 bgColor computed，不要试图外部覆盖。  [HIT:1]
