-- Create new database for Jewelry Shop Management System
CREATE DATABASE IF NOT EXISTS jewelryshop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user (optional - you can use existing user)
-- CREATE USER IF NOT EXISTS 'jewelryuser'@'localhost' IDENTIFIED BY 'jewelrypass123';
-- GRANT ALL PRIVILEGES ON jewelryshop.* TO 'jewelryuser'@'localhost';
-- FLUSH PRIVILEGES;

-- Use the database
USE jewelryshop;

-- The tables will be created automatically by Hibernate when you start the application
-- with spring.jpa.hibernate.ddl-auto=update
