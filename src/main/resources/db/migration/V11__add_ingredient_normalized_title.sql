ALTER TABLE ingredients
    ADD COLUMN normalized_title VARCHAR(100) NULL;

UPDATE ingredients
SET normalized_title = LOWER(TRIM(title));

ALTER TABLE ingredients
    MODIFY COLUMN normalized_title VARCHAR(100) NOT NULL;

ALTER TABLE ingredients
    ADD CONSTRAINT uk_ingredients_normalized_title UNIQUE (normalized_title);
