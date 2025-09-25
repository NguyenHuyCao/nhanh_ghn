-- V1__init.sql
SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- =========================
-- Table: users (admin + user chung)
-- =========================
CREATE TABLE IF NOT EXISTS users (
                                     id                   INT AUTO_INCREMENT PRIMARY KEY,
                                     created_at           DATETIME(6) NULL,
    updated_at           DATETIME(6) NULL,
    deleted              TINYINT(1) NOT NULL DEFAULT 0,

    name                 VARCHAR(255) NOT NULL,
    phone                TEXT,               -- AES (converter), không unique
    email                VARCHAR(255) NOT NULL,
    password             VARCHAR(255) NOT NULL,

    role_type            INT NOT NULL DEFAULT 1,  -- 0=ADMIN,1=USER,2=STUDENT
    status               INT NOT NULL DEFAULT 1,  -- 0=INACTIVE,1=ACTIVE

    failedLoginAttempts  INT NULL,
    lockedUntil          DATETIME(6) NULL,
    lastLoginAt          DATETIME(6) NULL,

    CONSTRAINT uk_users_email UNIQUE (email)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_status     ON users (status);
CREATE INDEX idx_users_role_type  ON users (role_type);

-- =========================
-- Table: upload_files
-- =========================
CREATE TABLE IF NOT EXISTS upload_files (
                                            id               INT AUTO_INCREMENT PRIMARY KEY,
                                            created_at       DATETIME(6) NULL,
    updated_at       DATETIME(6) NULL,
    deleted          TINYINT(1) NOT NULL DEFAULT 0,

    name             VARCHAR(255),
    originFilePath   TEXT,            -- trùng tên field entity
    originUrl        TEXT,            -- trùng tên field entity
    size             BIGINT,
    contentType      VARCHAR(255),    -- trùng tên field entity

    uploaded_by_id   INT NULL,
    CONSTRAINT fk_upload_files_user
    FOREIGN KEY (uploaded_by_id) REFERENCES users(id)
    ON DELETE SET NULL ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =========================
-- Table: configs (vì vẫn còn entity ConfigModel)
-- =========================
CREATE TABLE IF NOT EXISTS configs (
                                       id           INT AUTO_INCREMENT PRIMARY KEY,
                                       created_at   DATETIME(6) NULL,
    updated_at   DATETIME(6) NULL,
    deleted      TINYINT(1) NOT NULL DEFAULT 0,

    `key`        VARCHAR(64) NOT NULL,
    value        TEXT NULL,           -- @Lob TEXT
    description  VARCHAR(512),
    status       INT NOT NULL DEFAULT 1,

    CONSTRAINT uk_configs_key UNIQUE (`key`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
