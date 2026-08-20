ALTER TABLE recipes
    ADD COLUMN import_type VARCHAR(20);

ALTER TABLE recipes
    ADD COLUMN total_calories DECIMAL(10, 2);

ALTER TABLE recipes
    ADD CONSTRAINT ck_recipes_import_type
        CHECK (import_type IS NULL OR import_type IN ('URL', 'IMAGE', 'TEXT'));

ALTER TABLE recipes
    ADD CONSTRAINT ck_recipes_total_calories_non_negative
        CHECK (total_calories IS NULL OR total_calories >= 0);
