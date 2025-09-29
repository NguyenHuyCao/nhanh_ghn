-- V10: đảm bảo cột doanh thu của nhanh_order_items và cờ is_pr của ghn_orders
--      tồn tại, không NULL và có default = 0. Idempotent, an toàn cho DB đang chạy.

SET NAMES utf8mb4;
SET time_zone = '+00:00';

/* ---------- nhanh_order_items ---------- */
SET @tbl := 'nhanh_order_items';

-- Thêm cột nếu còn thiếu
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

-- Backfill về 0 cho tất cả NULL hiện có
UPDATE nhanh_order_items
SET
    discount_total = COALESCE(discount_total, 0),
    deposit_alloc  = COALESCE(deposit_alloc,  0),
    transfer_alloc = COALESCE(transfer_alloc, 0),
    revenue_item   = COALESCE(revenue_item,   0);

-- Khóa NOT NULL + DEFAULT 0 (sau khi đã backfill)
ALTER TABLE nhanh_order_items
    MODIFY COLUMN discount_total BIGINT NOT NULL DEFAULT 0,
    MODIFY COLUMN deposit_alloc  BIGINT NOT NULL DEFAULT 0,
    MODIFY COLUMN transfer_alloc BIGINT NOT NULL DEFAULT 0,
    MODIFY COLUMN revenue_item   BIGINT NOT NULL DEFAULT 0;

-- Index phụ trợ (nếu chưa có)
SET @idx := 'idx_noi_rev';
SET @sql := IF ((SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_noi_rev ON nhanh_order_items (order_id, sku, revenue_item)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;


/* ---------- ghn_orders ---------- */
SET @tbl := 'ghn_orders';

-- is_pr: thêm nếu thiếu
SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='is_pr')=0,
  'ALTER TABLE ghn_orders ADD COLUMN is_pr TINYINT(1) NOT NULL DEFAULT 0 AFTER order_code',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- Backfill (đề phòng cột đã có nhưng cho phép NULL)
UPDATE ghn_orders SET is_pr = COALESCE(is_pr, 0);

-- Chuẩn hóa NOT NULL + DEFAULT 0
ALTER TABLE ghn_orders
    MODIFY COLUMN is_pr TINYINT(1) NOT NULL DEFAULT 0;

-- Index lọc & sort cho màn /sheet/local
SET @idx := 'idx_go_ispr';
SET @sql := IF ((SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_go_ispr ON ghn_orders (is_pr, delivered_at)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
