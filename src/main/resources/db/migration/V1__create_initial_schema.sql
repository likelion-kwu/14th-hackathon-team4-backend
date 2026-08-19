CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_users_login_id UNIQUE (login_id)
);

CREATE TABLE health_profiles (
    profile_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    birth_date DATE,
    height DECIMAL(5, 2),
    weight DECIMAL(5, 2),
    sex VARCHAR(20),
    diabetes_status VARCHAR(30),
    glucose_device_connected BOOLEAN NOT NULL,
    daily_carbs_target INT,
    dietary_restriction_note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_health_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_health_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE ingredients (
    ingredient_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL
);

CREATE TABLE ingredient_nutritions (
    ingredient_nutrition_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ingredient_id BIGINT NOT NULL,
    calories DECIMAL(10, 2),
    carb DECIMAL(10, 2),
    protein DECIMAL(10, 2),
    fat DECIMAL(10, 2),
    fiber DECIMAL(10, 2),
    CONSTRAINT uk_ingredient_nutritions_ingredient_id UNIQUE (ingredient_id),
    CONSTRAINT fk_ingredient_nutritions_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (ingredient_id)
);

CREATE TABLE recipes (
    recipe_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    cooking_time INT,
    is_completed BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_recipes_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE recipe_steps (
    recipe_step_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT uk_recipe_steps_recipe_order UNIQUE (recipe_id, step_order),
    CONSTRAINT fk_recipe_steps_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipes (recipe_id)
);

CREATE TABLE recipe_ingredients (
    recipe_ingredient_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    CONSTRAINT uk_recipe_ingredient UNIQUE (recipe_id, ingredient_id),
    CONSTRAINT fk_recipe_ingredients_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipes (recipe_id),
    CONSTRAINT fk_recipe_ingredients_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (ingredient_id)
);

CREATE TABLE meal_logs (
    meal_log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT,
    image_url VARCHAR(500),
    meal_type VARCHAR(20) NOT NULL,
    glucose DECIMAL(6, 2),
    carb DECIMAL(10, 2),
    sugar DECIMAL(10, 2),
    eaten_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_meal_logs_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_meal_logs_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipes (recipe_id)
);
