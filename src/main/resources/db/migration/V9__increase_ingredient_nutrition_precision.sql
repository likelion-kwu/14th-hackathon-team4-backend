ALTER TABLE ingredient_nutritions
    MODIFY COLUMN calories DECIMAL(14, 6) NULL;

ALTER TABLE ingredient_nutritions
    MODIFY COLUMN carb DECIMAL(14, 6) NULL;

ALTER TABLE ingredient_nutritions
    MODIFY COLUMN protein DECIMAL(14, 6) NULL;

ALTER TABLE ingredient_nutritions
    MODIFY COLUMN fat DECIMAL(14, 6) NULL;

ALTER TABLE ingredient_nutritions
    MODIFY COLUMN fiber DECIMAL(14, 6) NULL;

ALTER TABLE ingredient_nutritions
    MODIFY COLUMN sugar DECIMAL(14, 6) NOT NULL;

ALTER TABLE ingredient_nutritions
    MODIFY COLUMN sodium DECIMAL(14, 6) NOT NULL;
