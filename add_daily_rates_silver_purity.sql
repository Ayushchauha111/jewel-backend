-- Add silver purity/accuracy to daily_rates (e.g. 92.5 for 92.5%, 999 for fineness). Run once when using ddl-auto=validate.
-- Omit if column already exists
ALTER TABLE daily_rates ADD COLUMN silver_purity_percentage DECIMAL(5,2) NULL AFTER silver_per_gram;
