ALTER TABLE meal_logs
    ADD COLUMN title VARCHAR(150) NULL;

ALTER TABLE meal_logs
    ADD COLUMN calories DECIMAL(10, 2) NULL;

UPDATE meal_logs
SET title = '기존 식사 기록'
WHERE title IS NULL;

ALTER TABLE meal_logs
    MODIFY COLUMN title VARCHAR(150) NOT NULL;

CREATE INDEX idx_meal_logs_user_eaten_at
    ON meal_logs (user_id, eaten_at);

CREATE TABLE glucose_records (
    glucose_record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    meal_log_id BIGINT,
    glucose_value DECIMAL(6, 2) NOT NULL,
    measurement_context VARCHAR(30) NOT NULL,
    measured_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT ck_glucose_records_value
        CHECK (glucose_value >= 20.00 AND glucose_value <= 600.00),
    CONSTRAINT fk_glucose_records_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_glucose_records_meal_log
        FOREIGN KEY (meal_log_id) REFERENCES meal_logs (meal_log_id)
);

CREATE INDEX idx_glucose_records_user_measured_at
    ON glucose_records (user_id, measured_at);
