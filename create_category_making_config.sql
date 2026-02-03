-- Create category_making_config table (required when using spring.jpa.hibernate.ddl-auto=validate)
-- Run once against your database, e.g. mysql -u user -p yourdb < create_category_making_config.sql
-- Or from MySQL client: USE yourdb; then paste the CREATE TABLE below.

CREATE TABLE IF NOT EXISTS category_making_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  category VARCHAR(100) NOT NULL,
  making_charges_per_gram DECIMAL(10,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
