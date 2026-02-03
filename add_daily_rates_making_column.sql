-- Add making_charges_per_gram to daily_rates (required when using spring.jpa.hibernate.ddl-auto=validate)
-- Run once against your database, e.g. mysql -u user -p yourdb < add_daily_rates_making_column.sql
-- If the column already exists, you will get "Duplicate column" and can ignore.

ALTER TABLE daily_rates
  ADD COLUMN making_charges_per_gram DECIMAL(10,2) NULL
  AFTER diamond_per_carat;
