-- =========================
-- V3: sheet sync schema (MySQL-safe, idempotent)
-- =========================

/* ---------- nhanh_orders ---------- */
CREATE TABLE IF NOT EXISTS nhanh_orders (
                                            id              BIGINT NOT NULL PRIMARY KEY,
                                            created_at      DATETIME NULL,
                                            customer_phone  VARCHAR(32),
    payment_channel VARCHAR(64),
    cod_to_collect  BIGINT,
    carrier         VARCHAR(64),
    carrier_code    VARCHAR(64),
    `status`        VARCHAR(32),
    created_ts      TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_ts      TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- add column helper
SET @tbl := 'nhanh_orders';

SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='created_at')=0,
  'ALTER TABLE nhanh_orders ADD COLUMN created_at DATETIME NULL',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='customer_phone')=0,
  'ALTER TABLE nhanh_orders ADD COLUMN customer_phone VARCHAR(32)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='payment_channel')=0,
  'ALTER TABLE nhanh_orders ADD COLUMN payment_channel VARCHAR(64)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='cod_to_collect')=0,
  'ALTER TABLE nhanh_orders ADD COLUMN cod_to_collect BIGINT',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='carrier')=0,
  'ALTER TABLE nhanh_orders ADD COLUMN carrier VARCHAR(64)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='carrier_code')=0,
  'ALTER TABLE nhanh_orders ADD COLUMN carrier_code VARCHAR(64)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='status')=0,
  'ALTER TABLE nhanh_orders ADD COLUMN `status` VARCHAR(32)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- indexes (no IF NOT EXISTS -> kiểm tra trước)
SET @idx := 'idx_no_carrier_code';
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_no_carrier_code ON nhanh_orders (carrier_code)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := 'idx_no_created_at';
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_no_created_at ON nhanh_orders (created_at)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;


/* ---------- nhanh_order_items ---------- */
CREATE TABLE IF NOT EXISTS nhanh_order_items (
                                                 id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                                 order_id   BIGINT NOT NULL,
                                                 sku        VARCHAR(128),
    `size`     VARCHAR(64),
    unit_price BIGINT,
    quantity   INT DEFAULT 1,
    UNIQUE KEY uk_noi_order_sku (order_id, sku),
    KEY idx_noi_order_id (order_id),
    KEY idx_noi_sku (sku)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @tbl := 'nhanh_order_items';

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='order_id')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN order_id BIGINT NOT NULL',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='sku')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN sku VARCHAR(128)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='size')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN `size` VARCHAR(64)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='unit_price')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN unit_price BIGINT',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='quantity')=0,
  'ALTER TABLE nhanh_order_items ADD COLUMN quantity INT DEFAULT 1',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;


/* ---------- ghn_orders ---------- */
CREATE TABLE IF NOT EXISTS ghn_orders (
                                          order_code         VARCHAR(64) NOT NULL PRIMARY KEY,
    client_order_code  VARCHAR(64),
    ship_status        VARCHAR(64),
    delivered_at       DATETIME NULL,
    ship_fee           BIGINT NULL,
    cod_amount         BIGINT NULL,
    return_note        VARCHAR(255)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

SET @tbl := 'ghn_orders';

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='client_order_code')=0,
  'ALTER TABLE ghn_orders ADD COLUMN client_order_code VARCHAR(64)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='ship_status')=0,
  'ALTER TABLE ghn_orders ADD COLUMN ship_status VARCHAR(64)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='delivered_at')=0,
  'ALTER TABLE ghn_orders ADD COLUMN delivered_at DATETIME NULL',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='ship_fee')=0,
  'ALTER TABLE ghn_orders ADD COLUMN ship_fee BIGINT NULL',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='cod_amount')=0,
  'ALTER TABLE ghn_orders ADD COLUMN cod_amount BIGINT NULL',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @sql := IF ((SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND COLUMN_NAME='return_note')=0,
  'ALTER TABLE ghn_orders ADD COLUMN return_note VARCHAR(255)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @idx := 'idx_go_delivered_at';
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME=@tbl AND INDEX_NAME=@idx)=0,
  'CREATE INDEX idx_go_delivered_at ON ghn_orders (delivered_at)',
  'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
