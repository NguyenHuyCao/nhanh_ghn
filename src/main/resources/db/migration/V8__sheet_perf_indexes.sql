-- V8: indexes tăng tốc /sheet/local
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- nhanh_orders: phục vụ WHERE created_at + ORDER BY created_at, id
SET @idx := 'idx_no_created_at_id';
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME='nhanh_orders'
       AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_no_created_at_id ON nhanh_orders (created_at DESC, id DESC)',
  'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- nhanh_order_items: JOIN và ORDER BY theo noi.id
SET @idx := 'idx_noi_order_id_id';
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME='nhanh_order_items'
       AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_noi_order_id_id ON nhanh_order_items (order_id, id)',
  'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ghn_orders: đã có PK(order_code) + idx delivered_at, giữ nguyên
SET @idx := 'idx_no_carrier_code';
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME='nhanh_orders'
       AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_no_carrier_code ON nhanh_orders (carrier_code)',
  'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;