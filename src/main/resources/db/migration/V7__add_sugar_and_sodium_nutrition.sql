ALTER TABLE ingredient_nutritions
    ADD COLUMN sugar DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE ingredient_nutritions
    ADD COLUMN sodium DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
