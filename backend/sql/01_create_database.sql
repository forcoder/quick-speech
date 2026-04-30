-- =============================================
-- Quick Speech 智能输入法 - 数据库初始化脚本
-- 数据库: PostgreSQL 15+
-- 执行顺序: 按编号顺序执行
-- =============================================

-- 创建数据库
CREATE DATABASE quick_speech
    WITH
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

\c quick_speech;

-- 创建Schema
CREATE SCHEMA IF NOT EXISTS quick_speech;
SET search_path TO quick_speech, public;

-- 创建UUID扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 创建更新时间自动触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';
