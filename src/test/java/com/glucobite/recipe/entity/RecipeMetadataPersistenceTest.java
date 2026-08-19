package com.glucobite.recipe.entity;

import com.glucobite.user.entity.User;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeMetadataPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

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

    private User persistUser(String loginId) {
        return entityManager.persistAndFlush(
                new User(loginId, "encoded-password", "레시피 사용자")
        );
    }
}
