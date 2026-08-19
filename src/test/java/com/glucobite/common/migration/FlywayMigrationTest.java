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
        boolean versionOneApplied = List.of(flyway.info().applied()).stream()
                .anyMatch(migration -> "1".equals(migration.getVersion().getVersion()));

        assertTrue(versionOneApplied);
        INITIAL_TABLES.forEach(table ->
                assertEquals(0L, jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table,
                        Long.class
                ))
        );
    }
}
