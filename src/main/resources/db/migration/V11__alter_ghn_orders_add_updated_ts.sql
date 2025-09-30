-- V11__alter_ghn_orders_add_updated_ts.sql
SET NAMES utf8mb4;
SET time_zone = '+00:00';

ALTER TABLE ghn_orders
    ADD COLUMN updated_ts TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE INDEX idx_go_updated_ts ON ghn_orders (updated_ts);
CREATE INDEX idx_go_ship_status ON ghn_orders (ship_status);
