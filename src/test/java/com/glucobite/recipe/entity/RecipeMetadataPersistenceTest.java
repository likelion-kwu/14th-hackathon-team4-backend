package com.glucobite.recipe.entity;

import com.glucobite.user.entity.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeMetadataPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsRecipeImportTypeAndTotalCalories() {
        User user = persistUser("metadata-user");
        Recipe recipe = entityManager.persistAndFlush(new Recipe(
                user,
                "두부 채소 덮밥",
                null,
                20,
                RecipeImportType.URL,
                new BigDecimal("430.50")
        ));
        entityManager.clear();

        Recipe savedRecipe = entityManager.find(Recipe.class, recipe.getId());

        assertThat(savedRecipe.getImportType()).isEqualTo(RecipeImportType.URL);
        assertThat(savedRecipe.getTotalCalories()).isEqualByComparingTo("430.50");
    }

    @Test
    void allowsNullMetadataForExistingOrUnanalyzedRecipes() {
        User user = persistUser("nullable-metadata-user");
        Recipe recipe = entityManager.persistAndFlush(
                new Recipe(user, "분석 전 레시피", null, null)
        );

        assertThat(recipe.getImportType()).isNull();
        assertThat(recipe.getTotalCalories()).isNull();
        assertThat(recipe.getRecipeType()).isEqualTo(RecipeType.BASE);
    }

    @Test
    void persistsPersonalizationCandidateMetadata() {
        User user = persistUser("candidate-metadata-user");
        Recipe source = entityManager.persistAndFlush(
                new Recipe(user, "계란 볶음밥", null, 20, RecipeImportType.TEXT, null)
        );
        Recipe candidate = entityManager.persistAndFlush(Recipe.personalizationCandidate(
                source,
                "닭가슴살 계란 볶음밥",
                "단백질을 보강한 수정안",
                20,
                new BigDecimal("420.00"),
                "고단백질 위주 수정안",
                "NUTRITION_BALANCE 목표 반영",
                "resp_test"
        ));
        entityManager.clear();

        Recipe saved = entityManager.find(Recipe.class, candidate.getId());

        assertThat(saved.getRecipeType()).isEqualTo(RecipeType.PERSONALIZATION_CANDIDATE);
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.getSourceRecipe().getId()).isEqualTo(source.getId());
        assertThat(saved.getPersonalizationLabel()).isEqualTo("고단백질 위주 수정안");
        assertThat(saved.getPersonalizationReason()).isEqualTo("NUTRITION_BALANCE 목표 반영");
        assertThat(saved.getOpenAIResponseId()).isEqualTo("resp_test");
    }

    @Test
    void completingRecipeMarksItAsPersonalized() {
        User user = persistUser("completed-type-user");
        Recipe recipe = new Recipe(user, "개인화 완료 레시피", null, 10);

        recipe.complete();
        Recipe saved = entityManager.persistAndFlush(recipe);

        assertThat(saved.isCompleted()).isTrue();
        assertThat(saved.getRecipeType()).isEqualTo(RecipeType.PERSONALIZED);
    }

    @Test
    void rejectsNegativeTotalCalories() {
        User user = persistUser("negative-calories-user");
        Recipe recipe = new Recipe(
                user,
                "잘못된 레시피",
                null,
                10,
                RecipeImportType.TEXT,
                new BigDecimal("-0.01")
        );

        assertThrows(ConstraintViolationException.class, () ->
                entityManager.persistAndFlush(recipe)
        );
    }

    @Test
    void exposesOnlyConfirmedImportTypes() {
        assertThat(RecipeImportType.values())
                .containsExactly(
                        RecipeImportType.URL,
                        RecipeImportType.IMAGE,
                        RecipeImportType.TEXT
                );
    }

    @Test
    void databaseRejectsUnknownImportType() {
        User user = persistUser("invalid-import-type-user");

        assertThrows(DataIntegrityViolationException.class, () ->
                insertRecipeMetadata(user.getId(), "FILE", null)
        );
    }

    @Test
    void databaseRejectsNegativeTotalCalories() {
        User user = persistUser("negative-db-calories-user");

        assertThrows(DataIntegrityViolationException.class, () ->
                insertRecipeMetadata(user.getId(), "TEXT", new BigDecimal("-0.01"))
        );
    }

    private User persistUser(String loginId) {
        return entityManager.persistAndFlush(
                new User(loginId, "encoded-password", "레시피 사용자")
        );
    }

    private void insertRecipeMetadata(Long userId, String importType, BigDecimal totalCalories) {
        jdbcTemplate.update(
                """
                INSERT INTO recipes (
                    user_id, title, is_completed, created_at, updated_at,
                    import_type, total_calories
                ) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
                """,
                userId,
                "제약조건 테스트 레시피",
                false,
                importType,
                totalCalories
        );
    }
}
