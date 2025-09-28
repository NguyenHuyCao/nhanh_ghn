-- V4: đảm bảo bảng nhanh_orders có cột id & PRIMARY KEY

-- thêm cột id nếu chưa có (đặt lên đầu bảng)
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'nhanh_orders'
       AND COLUMN_NAME = 'id') = 0,
  'ALTER TABLE nhanh_orders ADD COLUMN id BIGINT NOT NULL FIRST',
  'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- thêm PRIMARY KEY(id) nếu bảng chưa có PK
SET @sql := IF (
  (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'nhanh_orders'
       AND CONSTRAINT_TYPE = 'PRIMARY KEY') = 0,
  'ALTER TABLE nhanh_orders ADD PRIMARY KEY (id)',
  'SELECT 1'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
