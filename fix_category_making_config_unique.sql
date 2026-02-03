-- Fix duplicate key when adding same category with different material (e.g. Rings+Gold and Rings+Silver).
-- The table has a unique on category only; we need unique on (category, material).
--
-- Step A: Find the actual index name on your database. Run:
--   SHOW INDEX FROM category_making_config;
-- Look for an index where Key_name is a unique constraint and Column_name is only "category"
-- (not category + material). Note the Key_name (e.g. uk_category or something else).
--
-- Step B: Drop that index (replace YOUR_INDEX_NAME with the Key_name from Step A):
--   ALTER TABLE category_making_config DROP INDEX YOUR_INDEX_NAME;
--
-- Step C: Add unique on (category, material):
--   ALTER TABLE category_making_config ADD UNIQUE KEY uk_category_material (category, material);

-- Or run SHOW CREATE TABLE category_making_config; and look for UNIQUE KEY ... (category)
-- SHOW INDEX FROM category_making_config;

-- Drop the unique on category only (your SHOW INDEX showed Key_name = uk_category).
-- uk_category_material (category, material) already exists, so no ADD needed.
ALTER TABLE category_making_config DROP INDEX uk_category;
