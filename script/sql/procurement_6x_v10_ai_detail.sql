SET NAMES utf8mb4;
-- ============================================================
-- v10：验收单 AI 识别留痕
-- ============================================================
-- pms_acceptance 增加 ai_detail 列（LONGTEXT）：
--   JSON 数组，每轮 AI 发票识别追加一条：
--   [{"round":1,"time":"2026-09-02 10:00:00","files":["a.pdf"],"lines":["...报告行..."]}]
-- 前端表单保存时随验收单一起提交；详情/审批页可回看历史识别记录。
-- 幂等：列不存在时才添加
-- ============================================================

SELECT COUNT(*) INTO @cnt FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'pms_acceptance' AND column_name = 'ai_detail';
SET @sql = IF(@cnt = 0,
  'ALTER TABLE pms_acceptance ADD COLUMN ai_detail longtext NULL COMMENT "AI发票识别留痕(JSON数组,按轮次追加)"',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
