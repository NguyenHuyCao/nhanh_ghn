CREATE TABLE IF NOT EXISTS sync_state (
                                          source          VARCHAR(32) NOT NULL PRIMARY KEY,   -- 'nhanh' | 'ghn'
    last_created_at DATETIME NULL,
    last_id         BIGINT NULL,
    updated_ts      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
