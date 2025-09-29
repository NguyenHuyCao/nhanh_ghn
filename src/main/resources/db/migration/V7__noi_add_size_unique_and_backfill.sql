-- V7__noi_backfill_size_and_uniq.sql
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- 1) Backfill size từ hậu tố SKU nếu đang NULL (dạng ABC-XL, ABC-2XL, ...)
UPDATE nhanh_order_items
SET `size` = UPPER(SUBSTRING_INDEX(sku,'-',-1))
WHERE `size` IS NULL AND sku LIKE '%-%';

-- Chuẩn hoá vài alias
UPDATE nhanh_order_items SET `size`='2XL'  WHERE UPPER(`size`)='XXL';
UPDATE nhanh_order_items SET `size`='FREE' WHERE UPPER(`size`) IN ('F','FREESIZE');

-- 2) GOM BẢN GHI TRÙNG (order_id, sku, size) bằng cách giữ 1 dòng, cộng quantity
-- Tạo bảng tạm tổng hợp
CREATE TEMPORARY TABLE IF NOT EXISTS _noi_agg AS
SELECT
    MIN(id)        AS id_keep,
    order_id,
    sku,
    COALESCE(`size`,'') AS size_norm,
    SUM(COALESCE(quantity,1)) AS qty_sum,
    MAX(unit_price) AS unit_price_max
FROM nhanh_order_items
GROUP BY order_id, sku, COALESCE(`size`,'');  -- NULL coi như cùng 1 nhóm

-- Cập nhật dòng được giữ lại theo tổng hợp
UPDATE nhanh_order_items n
    JOIN _noi_agg a ON n.id = a.id_keep
    SET n.quantity = a.qty_sum,
        n.unit_price = a.unit_price_max;

-- Xoá những dòng dư (không phải id_keep)
DELETE n FROM nhanh_order_items n
LEFT JOIN _noi_agg a ON n.id = a.id_keep
WHERE a.id_keep IS NULL;

DROP TEMPORARY TABLE IF EXISTS _noi_agg;

-- 3) BỎ UNIQUE CŨ nếu có
SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME='nhanh_order_items'
               AND INDEX_NAME='uk_noi_order_sku');
SET @sql := IF(@has>0, 'ALTER TABLE nhanh_order_items DROP INDEX uk_noi_order_sku', 'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) THÊM UNIQUE MỚI (order_id, sku, size) nếu chưa có
SET @has2 := (SELECT COUNT(*) FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME='nhanh_order_items'
                AND INDEX_NAME='uk_noi_order_sku_size');
SET @sql := IF(@has2=0,
  'ALTER TABLE nhanh_order_items ADD UNIQUE KEY uk_noi_order_sku_size (order_id, sku, `size`)',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
