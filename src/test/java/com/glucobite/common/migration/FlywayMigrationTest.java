package com.glucobite.common.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FlywayMigrationTest {

    private static final List<String> INITIAL_TABLES = List.of(
            "users",
            "health_profiles",
            "ingredients",
            "ingredient_nutritions",
            "recipes",
            "recipe_steps",
            "recipe_ingredients",
            "meal_logs"
    );

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesInitialMigration() {
        assertTrue(isApplied("1"));
        INITIAL_TABLES.forEach(table ->
                assertEquals(0L, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table,
                        Long.class
                ))
        );
    }

    @Test
    void appliesOnboardingMigrationsAndSeedsAllergens() {
        assertTrue(isApplied("2"));
        assertTrue(isApplied("3"));
        assertEquals(19L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM allergens",
                Long.class
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_profile_allergies",
                Long.class
        ));
    }

    private boolean isApplied(String version) {
        return List.of(flyway.info().applied()).stream()
                .anyMatch(migration -> version.equals(migration.getVersion().getVersion()));
    }
}
