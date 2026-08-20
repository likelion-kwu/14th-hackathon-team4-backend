CREATE TABLE ingredient_substitutes (
    ingredient_substitute_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id BIGINT NOT NULL,
    substitute_ingredient_id BIGINT NOT NULL,
    ratio DECIMAL(10, 4) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_ingredient_substitute UNIQUE (ingredient_id, substitute_ingredient_id),
    CONSTRAINT ck_ingredient_substitute_ratio_positive CHECK (ratio > 0),
    CONSTRAINT ck_ingredient_substitute_distinct CHECK (ingredient_id <> substitute_ingredient_id),
    CONSTRAINT fk_ingredient_substitute_original
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (ingredient_id),
    CONSTRAINT fk_ingredient_substitute_substitute
        FOREIGN KEY (substitute_ingredient_id) REFERENCES ingredients (ingredient_id)
);
