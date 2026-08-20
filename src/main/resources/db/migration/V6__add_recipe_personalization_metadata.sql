ALTER TABLE recipes
    ADD COLUMN recipe_type VARCHAR(40);

UPDATE recipes
SET recipe_type = CASE
    WHEN is_completed = TRUE THEN 'PERSONALIZED'
    ELSE 'BASE'
END;

ALTER TABLE recipes
    MODIFY COLUMN recipe_type VARCHAR(40) NOT NULL;

ALTER TABLE recipes
    ADD COLUMN source_recipe_id BIGINT;

ALTER TABLE recipes
    ADD COLUMN personalization_label VARCHAR(150);

ALTER TABLE recipes
    ADD COLUMN personalization_reason VARCHAR(500);

ALTER TABLE recipes
    ADD COLUMN openai_response_id VARCHAR(100);

ALTER TABLE recipes
    ADD CONSTRAINT ck_recipes_recipe_type
        CHECK (recipe_type IN ('BASE', 'PERSONALIZATION_CANDIDATE', 'PERSONALIZED'));

ALTER TABLE recipes
    ADD CONSTRAINT fk_recipes_source_recipe
        FOREIGN KEY (source_recipe_id) REFERENCES recipes (recipe_id);

CREATE INDEX idx_recipes_source_recipe_id
    ON recipes (source_recipe_id);

CREATE INDEX idx_recipes_user_type_created
    ON recipes (user_id, recipe_type, created_at, recipe_id);
