ALTER TABLE health_profiles
    ADD COLUMN health_goal VARCHAR(40) NOT NULL;

ALTER TABLE health_profiles
    ADD COLUMN vegetarian_type VARCHAR(30) NOT NULL;
