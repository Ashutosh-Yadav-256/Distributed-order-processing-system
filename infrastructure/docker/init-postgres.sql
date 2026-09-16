-- Distributed Order Processing System
-- Multi-database initialization script for PostgreSQL
-- Creates isolated logical databases per microservice

SELECT 'CREATE DATABASE order_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'order_db')\gexec

SELECT 'CREATE DATABASE inventory_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db')\gexec

SELECT 'CREATE DATABASE payment_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')\gexec

SELECT 'CREATE DATABASE notification_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec

-- Grant privileges to postgres user
GRANT ALL PRIVILEGES ON DATABASE order_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE inventory_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE payment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
