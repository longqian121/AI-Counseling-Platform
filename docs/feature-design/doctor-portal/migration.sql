-- 心理医生门户 - 数据库迁移
-- 执行前请确保已备份数据

ALTER TABLE counselor ADD COLUMN user_id BIGINT AFTER id;
ALTER TABLE counselor ADD INDEX idx_user_id (user_id);
