-- V6: thêm cột ngân hàng cho ghn_orders (idempotent)
SET NAMES utf8mb4;
SET time_zone = '+00:00';

SET @tbl := 'ghn_orders';

-- bank_collected_at
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='bank_collected_at')=0,
  'ALTER TABLE ghn_orders ADD COLUMN bank_collected_at DATETIME NULL',
  'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- bank_amount
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='bank_amount')=0,
  'ALTER TABLE ghn_orders ADD COLUMN bank_amount BIGINT NULL',
  'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
