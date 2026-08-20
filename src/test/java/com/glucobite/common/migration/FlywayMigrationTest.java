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
                assertEquals(1L, jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = ?
                        """,
                        Long.class,
                        table
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

    @Test
    void appliesRecipeListMetadataMigration() {
        assertTrue(isApplied("4"));
        assertEquals(2L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'recipes'
                  AND column_name IN ('import_type', 'total_calories')
                """,
                Long.class
        ));
    }

    @Test
    void appliesRecipePersonalizationMetadataMigration() {
        assertTrue(isApplied("6"));
        assertEquals(5L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'recipes'
                  AND column_name IN (
                    'recipe_type',
                    'source_recipe_id',
                    'personalization_label',
                    'personalization_reason',
                    'openai_response_id'
                  )
                """,
                Long.class
        ));
    }

    @Test
    void appliesSugarAndSodiumNutritionMigration() {
        assertTrue(isApplied("7"));
        assertEquals(2L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'ingredient_nutritions'
                  AND column_name IN ('sugar', 'sodium')
                """,
                Long.class
        ));
    }

    @Test
    void appliesRecipeSourceMetadataMigration() {
        assertTrue(isApplied("8"));
        assertEquals(3L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'recipes'
                  AND column_name IN ('source_url', 'source_external_id', 'image_url')
                """,
                Long.class
        ));
    }

    @Test
    void appliesIngredientNutritionPrecisionMigration() {
        assertTrue(isApplied("9"));
        assertEquals(7L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'ingredient_nutritions'
                  AND column_name IN (
                    'calories', 'carb', 'protein', 'fat', 'fiber', 'sugar', 'sodium'
                  )
                  AND numeric_precision = 14
                  AND numeric_scale = 6
                """,
                Long.class
        ));
    }

    @Test
    void appliesRecipeIngredientNutritionSnapshotMigration() {
        assertTrue(isApplied("10"));
        assertEquals(7L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'recipe_ingredients'
                  AND column_name IN (
                    'calories_per_gram', 'carb_per_gram', 'protein_per_gram',
                    'fat_per_gram', 'fiber_per_gram', 'sugar_per_gram', 'sodium_per_gram'
                  )
                  AND numeric_precision = 14
                  AND numeric_scale = 6
                """,
                Long.class
        ));
    }

    @Test
    void appliesIngredientNormalizedTitleMigration() {
        assertTrue(isApplied("11"));
        assertEquals(1L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'ingredients'
                  AND column_name = 'normalized_title'
                  AND is_nullable = 'NO'
                """,
                Long.class
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'ingredients'
                  AND constraint_name = 'uk_ingredients_normalized_title'
                  AND constraint_type = 'UNIQUE'
                """,
                Long.class
        ));
    }

    @Test
    void appliesRecipeImportDedupeKeyMigration() {
        assertTrue(isApplied("12"));
        assertEquals(1L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'recipes'
                  AND column_name = 'import_dedupe_key'
                  AND is_nullable = 'YES'
                """,
                Long.class
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'recipes'
                  AND constraint_name = 'uk_recipes_user_import_dedupe_key'
                  AND constraint_type = 'UNIQUE'
                """,
                Long.class
        ));
    }

    private boolean isApplied(String version) {
        return List.of(flyway.info().applied()).stream()
                .anyMatch(migration -> version.equals(migration.getVersion().getVersion()));
    }
}
