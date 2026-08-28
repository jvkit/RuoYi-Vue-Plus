SET NAMES utf8mb4;
-- ============================================================
-- 采购管理：流程分类 + 流程定义分类关联（ruoyi-6x / ry-vue-6x）
-- 目的：
--   1. 在 flow_category 中新增「采购」顶级分类 +「采购申请」子分类，
--      使「我的任务 / 流程分类」下拉与列表能正确显示采购板块（此前只有 OA审批）。
--   2. 将 pms_request 流程定义的 category 从错误的 '100' 更新为新分类 id。
-- 可重复执行（幂等）：分类先删后插、流程定义 UPDATE 按 flow_code 定位。
-- 导入库：ry-vue-6x
-- ============================================================

SET @admin_id = 1761100000000000001; -- admin 用户 id
SET @dept_id  = 1761000000000000103; -- 主部门 id

-- 分类 id（避开既有 1762300000000000100~109，另起一段）
SET @cat_procure  = 1762300000000000200; -- 采购（顶级）
SET @cat_req      = 1762300000000000201; -- 采购申请（子分类）

-- ============================================================
-- 1. 流程分类：先删后插，保证幂等
-- ============================================================
DELETE FROM flow_category WHERE category_id IN (@cat_procure, @cat_req);

INSERT INTO flow_category (category_id, parent_id, ancestors, category_name, order_num, del_flag, create_dept, create_by, create_time, update_by, update_time)
VALUES (@cat_procure, 0, '0', '采购', 2, '0', @dept_id, @admin_id, sysdate(), NULL, NULL);

INSERT INTO flow_category (category_id, parent_id, ancestors, category_name, order_num, del_flag, create_dept, create_by, create_time, update_by, update_time)
VALUES (@cat_req, @cat_procure, CONCAT('0,', @cat_procure), '采购申请', 0, '0', @dept_id, @admin_id, sysdate(), NULL, NULL);

-- ============================================================
-- 2. pms_request 流程定义关联到「采购申请」分类
--    （此前 category='100' 并非有效分类 id，导致流程分类显示为空）
-- ============================================================
UPDATE flow_definition
SET category = CAST(@cat_req AS CHAR)
WHERE flow_code = 'pms_request' AND tenant_id = '000000';

-- ============================================================
-- 3. 流程定义 form_path + 审批详情菜单（「我的任务」办理/查看跳转）
--    待办/已办 SQL 的 formPath 取 flow_definition.form_path（d.form_path），
--    配为采购审批详情页路由；详情页 component 需存在于前端并注册隐藏菜单。
-- ============================================================
UPDATE flow_definition SET form_path = '/procurement/request/detail'
WHERE flow_code = 'pms_request' AND tenant_id = '000000';

-- 隐藏菜单：采购申请审批详情（注册前端路由 /procurement/request/detail）
-- 注意：必须挂在「采购管理」目录(M)下、path 带子路径(request/detail)，目录才会递归生成路由
DELETE FROM sys_menu WHERE menu_id = 1801046;
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time, update_by, update_time, remark)
VALUES (1801046, '采购申请审批详情', 1801000, 20, 'request/detail', 'procurement/request/detail', NULL, 'N', 'Y', 'C', '1', '0', 'procurement:request:query', 'form', NULL, NULL, @dept_id, @admin_id, sysdate(), NULL, NULL, '待办/已办办理与查看跳转页（隐藏）');

-- ============================================================
-- 4. 校验
-- ============================================================
SELECT category_id, parent_id, category_name FROM flow_category WHERE category_id IN (@cat_procure, @cat_req) ORDER BY category_id;
SELECT flow_code, category, form_path FROM flow_definition WHERE flow_code = 'pms_request';
SELECT menu_id, menu_name, parent_id, path, component, visible FROM sys_menu WHERE menu_id = 1801046;
