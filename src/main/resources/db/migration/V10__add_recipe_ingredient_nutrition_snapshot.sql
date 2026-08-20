ALTER TABLE recipe_ingredients
    ADD COLUMN calories_per_gram DECIMAL(14, 6) NULL;

ALTER TABLE recipe_ingredients
    ADD COLUMN carb_per_gram DECIMAL(14, 6) NULL;

ALTER TABLE recipe_ingredients
    ADD COLUMN protein_per_gram DECIMAL(14, 6) NULL;

ALTER TABLE recipe_ingredients
    ADD COLUMN fat_per_gram DECIMAL(14, 6) NULL;

ALTER TABLE recipe_ingredients
    ADD COLUMN fiber_per_gram DECIMAL(14, 6) NULL;

ALTER TABLE recipe_ingredients
    ADD COLUMN sugar_per_gram DECIMAL(14, 6) NULL;

ALTER TABLE recipe_ingredients
    ADD COLUMN sodium_per_gram DECIMAL(14, 6) NULL;
