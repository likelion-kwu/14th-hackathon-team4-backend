ALTER TABLE recipes
    ADD COLUMN import_dedupe_key VARCHAR(150) NULL;

ALTER TABLE recipes
    ADD CONSTRAINT uk_recipes_user_import_dedupe_key
        UNIQUE (user_id, import_dedupe_key);
