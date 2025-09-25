-- V2__add_refresh_tokens.sql
SET NAMES utf8mb4;
SET time_zone = '+00:00';

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              user_id INT NOT NULL,
                                              token VARCHAR(300) NOT NULL,
    expiresAt DATETIME(6) NOT NULL,
    revokedAt DATETIME(6) NULL,
    replacedBy VARCHAR(300) NULL,
    userAgent VARCHAR(512) NULL,
    ip VARCHAR(64) NULL,
    CONSTRAINT uk_rt_token UNIQUE (token),
    CONSTRAINT fk_rt_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_rt_user ON refresh_tokens(user_id);
CREATE INDEX idx_rt_token ON refresh_tokens(token);
