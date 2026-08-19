ALTER TABLE health_profiles
    ADD COLUMN health_goal VARCHAR(40) NOT NULL;

ALTER TABLE health_profiles
    ADD COLUMN vegetarian_type VARCHAR(30) NOT NULL;

ALTER TABLE health_profiles
    MODIFY COLUMN birth_date DATE NOT NULL;

ALTER TABLE health_profiles
    MODIFY COLUMN height DECIMAL(5, 2) NOT NULL;

ALTER TABLE health_profiles
    MODIFY COLUMN weight DECIMAL(5, 2) NOT NULL;

ALTER TABLE health_profiles
    MODIFY COLUMN sex VARCHAR(20) NOT NULL;

ALTER TABLE health_profiles
    MODIFY COLUMN daily_carbs_target INT NOT NULL;
