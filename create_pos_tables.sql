-- POS feature tables (optional: Hibernate ddl-auto=update creates these automatically)
-- Day open/close
-- CREATE TABLE day_sessions (...);

-- Returns/exchange
-- CREATE TABLE billing_returns (...);
-- CREATE TABLE billing_return_items (...);

-- Promo codes
-- CREATE TABLE promo_codes (...);

-- Gift vouchers
-- CREATE TABLE gift_vouchers (...);

-- Layaway
-- CREATE TABLE layaways (...);
-- CREATE TABLE layaway_items (...);
-- CREATE TABLE layaway_payments (...);

-- Loyalty
-- ALTER TABLE customers ADD COLUMN loyalty_points DECIMAL(12,2) DEFAULT 0;
-- CREATE TABLE loyalty_transactions (...);

-- Run application with spring.jpa.hibernate.ddl-auto=update to create/alter tables.
