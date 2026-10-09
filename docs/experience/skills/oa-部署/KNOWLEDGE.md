# oa-部署 — experience

> Shared cross-session lessons for `oa-部署`.
> Auto-maintained by fireworks-skill-memory. Last updated: 2026-10-02

## Entries

- [2026-10] [set -e 脚本算术自增] `((x++))` 在 `set -euo pipefail` 下 x=0 时表达式值为 0、返回状态 1，直接误杀脚本 —— 一律改写 `x=$((x+1))`；apply-sql.sh 曾因此每个新 SQL 执行成功并写日志后即退出，报「SQL 执行失败」，每重跑一次只推进一个脚本，极具迷惑性。  [HIT:1]
- [2026-10] [bash 按名杀进程] `pkill -f <关键词>` 会把发起命令的 shell 自身命令行也匹配上导致自杀（退出码 -1 无输出）—— 用 `pkill -f '[v]ite'` 这种首字符加括号的写法让模式不匹配自身。  [HIT:1]
