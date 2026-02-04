-- Add payment_breakdown column for split payment (e.g. cash + UPI)
-- Run once. If column already exists, skip or use: ALTER TABLE billing ADD COLUMN payment_breakdown VARCHAR(2000) NULL;
ALTER TABLE billing ADD COLUMN payment_breakdown VARCHAR(2000) NULL;
