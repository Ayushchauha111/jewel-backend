-- Add material to category_making_config (category + material). Run once when using ddl-auto=validate.
-- Existing rows: material = NULL (category-only default).
-- Unique: (category, material) so you can have e.g. Rings+Gold, Rings+Silver, Rings (default).

-- 1) Add column (nullable for existing rows). Omit if column already exists.
ALTER TABLE category_making_config ADD COLUMN material VARCHAR(50) NULL AFTER category;

-- 2) Drop old unique on category only
ALTER TABLE category_making_config DROP INDEX uk_category;

-- 3) Add new unique on (category, material)
ALTER TABLE category_making_config ADD UNIQUE KEY uk_category_material (category, material);
