-- VÁ NÓNG V9: thêm cột doanh thu + cờ GHN _PR (idempotent)

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- 1) nhanh_order_items: thêm cột nếu thiếu
SET @tbl := 'nhanh_order_items';

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='discount_total')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN discount_total BIGINT NULL AFTER quantity',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='deposit_alloc')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN deposit_alloc BIGINT NULL AFTER discount_total',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='transfer_alloc')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN transfer_alloc BIGINT NULL AFTER deposit_alloc',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='revenue_item')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN revenue_item BIGINT NULL AFTER transfer_alloc',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- chỉ mục phụ trợ (tùy DB có hay chưa)
SET @idx := 'idx_noi_rev';
SET @sql := IF ((SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_noi_rev ON nhanh_order_items (order_id, sku, revenue_item)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) ghn_orders: thêm is_pr nếu thiếu
SET @tbl := 'ghn_orders';
SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='is_pr')=0,
  'ALTER TABLE ghn_orders ADD COLUMN is_pr TINYINT(1) NOT NULL DEFAULT 0 AFTER order_code',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- bảo đảm có UNIQUE để ON DUPLICATE KEY hoạt động theo order_code
SET @sql := IF ((SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND NON_UNIQUE=0 AND INDEX_NAME IN ('PRIMARY','ux_go_order_code'))=0,
  'CREATE UNIQUE INDEX ux_go_order_code ON ghn_orders (order_code)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- chỉ mục filter nhanh cho is_pr
SET @idx := 'idx_go_ispr';
SET @sql := IF ((SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_go_ispr ON ghn_orders (is_pr, delivered_at)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
