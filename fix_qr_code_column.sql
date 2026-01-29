-- Fix QR code column size in stock table
-- Run this if the table was already created with the old column size

USE jewel;

ALTER TABLE stock MODIFY COLUMN qr_code TEXT;
