ALTER TABLE recipes
    DROP CONSTRAINT ck_recipes_import_type;

ALTER TABLE recipes
    DROP CONSTRAINT ck_recipes_recipe_type;

ALTER TABLE recipes
    ADD COLUMN source_url VARCHAR(500) NULL;

ALTER TABLE recipes
    ADD COLUMN source_external_id VARCHAR(100) NULL;

ALTER TABLE recipes
    ADD COLUMN image_url VARCHAR(500) NULL;

ALTER TABLE recipes
    ADD CONSTRAINT ck_recipes_import_type
        CHECK (import_type IS NULL OR import_type IN ('URL', 'IMAGE', 'TEXT'));

ALTER TABLE recipes
    ADD CONSTRAINT ck_recipes_recipe_type
        CHECK (recipe_type IN ('BASE', 'PERSONALIZATION_CANDIDATE', 'PERSONALIZED'));

CREATE INDEX idx_recipes_user_source
    ON recipes (user_id, import_type, source_external_id);
