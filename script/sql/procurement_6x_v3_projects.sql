SET NAMES utf8mb4;
-- ============================================================
-- 采购 v3：项目初始化数据（幂等，可重复执行）
--   结构（对应部门 = 资金来源）：
--     长三角(1761000000000000101)
--       ├─ 本地隐私保护项目
--       │    ├─ RAG
--       │    └─ Agent
--       └─ AI平台
--     天目湖(1761000000000000102)
--       └─ 全画幅结构光超分辨显微镜系统
--            ├─ SIM显微镜系统-电路部分
--            ├─ SIM显微镜系统-光学部分
--            ├─ 活细胞工作站
--            ├─ 高速液晶相位调制仪
--            ├─ Filter Wheel
--            ├─ 准直激光器
--            └─ 大型活细胞工作站
--     北京(2088182564548644865)：暂无项目
--   幂等策略：
--     * 父项目按「名称+parent_id=0」查重，已存在则复用其 id（兼容历史数据），不存在才新建
--     * 子项目按「名称+父id」查重，不存在才插入
--     * 全脚本可重复执行
-- ============================================================
SET @admin_id = 1761100000000000001;
SET @dept_csj = 1761000000000000101;  -- 长三角
SET @dept_thl = 1761000000000000102;  -- 天目湖

-- ---------- 长三角 ----------
-- 本地隐私保护项目：查已有，无则新建（固定 id 2090000000000000001）
SET @p_csj_local = (SELECT id FROM pms_project WHERE project_name='本地隐私保护项目' AND parent_id=0 AND del_flag='0' LIMIT 1);
INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000001, 0, 'purp-init-csj-local', '本地隐私保护项目', @dept_csj, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE @p_csj_local IS NULL;
SET @p_csj_local = COALESCE((SELECT id FROM pms_project WHERE project_name='本地隐私保护项目' AND parent_id=0 AND del_flag='0' LIMIT 1), 2090000000000000001);

-- RAG（挂 @p_csj_local 下）
INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000002, @p_csj_local, 'purp-init-csj-rag', 'RAG', @dept_csj, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='RAG' AND parent_id=@p_csj_local AND del_flag='0');

-- Agent（挂 @p_csj_local 下）
INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000003, @p_csj_local, 'purp-init-csj-agent', 'Agent', @dept_csj, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='Agent' AND parent_id=@p_csj_local AND del_flag='0');

-- AI平台（顶级）
INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000004, 0, 'purp-init-csj-ai', 'AI平台', @dept_csj, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='AI平台' AND parent_id=0 AND del_flag='0');

-- ---------- 天目湖 ----------
-- 全画幅结构光超分辨显微镜系统（顶级）
SET @p_thl_mic = (SELECT id FROM pms_project WHERE project_name='全画幅结构光超分辨显微镜系统' AND parent_id=0 AND del_flag='0' LIMIT 1);
INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000010, 0, 'purp-init-thl-mic', '全画幅结构光超分辨显微镜系统', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE @p_thl_mic IS NULL;
SET @p_thl_mic = COALESCE((SELECT id FROM pms_project WHERE project_name='全画幅结构光超分辨显微镜系统' AND parent_id=0 AND del_flag='0' LIMIT 1), 2090000000000000010);

-- 子项目（挂 @p_thl_mic 下）
INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000011, @p_thl_mic, 'purp-init-thl-circ', 'SIM显微镜系统-电路部分', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='SIM显微镜系统-电路部分' AND parent_id=@p_thl_mic AND del_flag='0');

INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000012, @p_thl_mic, 'purp-init-thl-opt', 'SIM显微镜系统-光学部分', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='SIM显微镜系统-光学部分' AND parent_id=@p_thl_mic AND del_flag='0');

INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000013, @p_thl_mic, 'purp-init-thl-cell', '活细胞工作站', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='活细胞工作站' AND parent_id=@p_thl_mic AND del_flag='0');

INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000014, @p_thl_mic, 'purp-init-thl-mod', '高速液晶相位调制仪', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='高速液晶相位调制仪' AND parent_id=@p_thl_mic AND del_flag='0');

INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000015, @p_thl_mic, 'purp-init-thl-fw', 'Filter Wheel', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='Filter Wheel' AND parent_id=@p_thl_mic AND del_flag='0');

INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000016, @p_thl_mic, 'purp-init-thl-laser', '准直激光器', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='准直激光器' AND parent_id=@p_thl_mic AND del_flag='0');

INSERT INTO pms_project (id, parent_id, project_code, project_name, dept_id, leader, leader_id, budget, used_amount, status, remark, create_by, create_time, del_flag)
SELECT 2090000000000000017, @p_thl_mic, 'purp-init-thl-big', '大型活细胞工作站', @dept_thl, NULL, NULL, 0, 0, 1, '初始化数据', @admin_id, sysdate(), '0'
WHERE NOT EXISTS (SELECT 1 FROM pms_project WHERE project_name='大型活细胞工作站' AND parent_id=@p_thl_mic AND del_flag='0');

-- 校验：全项目树
SELECT id, parent_id, project_name, dept_id FROM pms_project WHERE del_flag='0' ORDER BY parent_id, id;
