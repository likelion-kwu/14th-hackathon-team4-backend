CREATE TABLE recipe_substitution_suggestions (
    suggestion_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    original_ingredient_id BIGINT NOT NULL,
    substitute_ingredient_id BIGINT NOT NULL,
    request_key VARCHAR(64) NOT NULL,
    suggestion_order INT NOT NULL,
    user_input VARCHAR(300) NOT NULL,
    recommended_amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    warning VARCHAR(500),
    calories_per_gram DECIMAL(14, 6) NOT NULL,
    carb_per_gram DECIMAL(14, 6) NOT NULL,
    protein_per_gram DECIMAL(14, 6) NOT NULL,
    fat_per_gram DECIMAL(14, 6) NOT NULL,
    fiber_per_gram DECIMAL(14, 6) NOT NULL,
    sugar_per_gram DECIMAL(14, 6) NOT NULL,
    sodium_per_gram DECIMAL(14, 6) NOT NULL,
    openai_response_id VARCHAR(100),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_recipe_sub_suggestion_request_order UNIQUE (
        user_id,
        recipe_id,
        original_ingredient_id,
        request_key,
        suggestion_order
    ),
    CONSTRAINT fk_recipe_sub_suggestions_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_recipe_sub_suggestions_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipes (recipe_id),
    CONSTRAINT fk_recipe_sub_suggestions_original
        FOREIGN KEY (original_ingredient_id) REFERENCES ingredients (ingredient_id),
    CONSTRAINT fk_recipe_sub_suggestions_substitute
        FOREIGN KEY (substitute_ingredient_id) REFERENCES ingredients (ingredient_id)
);

CREATE TABLE recipe_substitution_suggestion_sources (
    suggestion_source_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    suggestion_id BIGINT NOT NULL,
    source_order INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    CONSTRAINT uk_recipe_sub_suggestion_source_order UNIQUE (
        suggestion_id,
        source_order
    ),
    CONSTRAINT fk_recipe_sub_suggestion_sources_suggestion
        FOREIGN KEY (suggestion_id)
        REFERENCES recipe_substitution_suggestions (suggestion_id)
);
