-- =====================================================
-- DevOps Practice Database Setup Script (MySQL)
-- Usage: mysql -u root -p < database/init.sql
-- =====================================================

-- 1. Create the database
CREATE DATABASE IF NOT EXISTS devops_practice_db;

-- 2. Use the database
USE devops_practice_db;

-- =====================================================
-- TABLES
-- =====================================================

-- items table (used by the backend /api/items endpoints)
CREATE TABLE IF NOT EXISTS items (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- users table (example for future auth practice)
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- SEED DATA
-- =====================================================

INSERT INTO items (name) VALUES
    ('Learn Docker'),
    ('Setup Kubernetes'),
    ('Configure CI/CD'),
    ('Deploy to AWS'),
    ('Monitor with Prometheus'),
    ('Practice Terraform'),
    ('Write a GitHub Action');

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_items_created_at ON items(created_at);
CREATE INDEX idx_users_email ON users(email);