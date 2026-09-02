#!/bin/bash
# 在服务器上执行 procurement SQL 脚本
set -uo pipefail

DB="ry-vue-6x"
USER="root"
PASS="ruoyi123"

run_sql() {
    local file=$1
    echo "执行 $file ..."
    docker exec -i ruoyi-mysql mysql -u$USER -p$PASS -D $DB --default-character-set=utf8mb4 < "$file" || echo "跳过错误: $file"
}

run_sql /tmp/procurement_6x.sql
run_sql /tmp/procurement_6x_increment.sql
run_sql /tmp/procurement_6x_category.sql
run_sql /tmp/procurement_6x_v2_tables.sql
run_sql /tmp/procurement_6x_v2.sql
run_sql /tmp/procurement_6x_v3.sql
run_sql /tmp/procurement_6x_v3_projects.sql
run_sql /tmp/procurement_6x_v3_flow.sql
run_sql /tmp/procurement_6x_v3_common.sql
run_sql /tmp/procurement_6x_v3_acceptance_flow.sql
run_sql /tmp/procurement_6x_v3_acceptance_menu.sql
run_sql /tmp/procurement_6x_v3_acceptance_flag.sql
run_sql /tmp/procurement_6x_v3_issue_flow.sql
run_sql /tmp/procurement_fund.sql
run_sql /tmp/procurement_6x_common_user_perms.sql
run_sql /tmp/procurement_6x_oss_query_permission.sql
run_sql /tmp/procurement_6x_mobile_acceptance_menu.sql
run_sql /tmp/procurement_6x_workflow_instance_query_permission.sql

echo "所有 SQL 执行完成"
