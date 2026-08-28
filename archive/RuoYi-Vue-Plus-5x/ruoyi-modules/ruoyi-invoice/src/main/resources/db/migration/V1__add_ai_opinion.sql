-- 为发票表添加 AI 审核意见字段
ALTER TABLE `invoice_info` ADD COLUMN `ai_opinion` TEXT COMMENT 'AI审核意见' AFTER `remark`;