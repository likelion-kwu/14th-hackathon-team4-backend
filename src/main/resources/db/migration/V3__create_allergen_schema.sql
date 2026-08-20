CREATE TABLE allergens (
    allergen_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_allergens_name UNIQUE (name)
);

CREATE TABLE health_profile_allergies (
    profile_id BIGINT NOT NULL,
    allergen_id BIGINT NOT NULL,
    CONSTRAINT pk_health_profile_allergies PRIMARY KEY (profile_id, allergen_id),
    CONSTRAINT fk_health_profile_allergies_profile
        FOREIGN KEY (profile_id) REFERENCES health_profiles (profile_id),
    CONSTRAINT fk_health_profile_allergies_allergen
        FOREIGN KEY (allergen_id) REFERENCES allergens (allergen_id)
);

INSERT INTO allergens (name, created_at, updated_at) VALUES
    ('난류', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('우유', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('메밀', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('땅콩', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('대두', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('밀', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('고등어', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('게', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('새우', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('돼지고기', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('복숭아', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('토마토', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('아황산류', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('호두', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('닭고기', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('쇠고기', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('오징어', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('조개류', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('잣', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
