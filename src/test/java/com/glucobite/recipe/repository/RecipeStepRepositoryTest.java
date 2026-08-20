package com.glucobite.recipe.repository;

import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeStep;
import com.glucobite.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeStepRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecipeStepRepository recipeStepRepository;

    @Test
    void returnsStepsSortedByStepOrderAsc() {
        User user = entityManager.persistAndFlush(
                new User("step-user", "encoded-password", "단계 사용자")
        );
        Recipe recipe = entityManager.persistAndFlush(
                new Recipe(user, "정렬 테스트", null, 10)
        );
        entityManager.persistAndFlush(new RecipeStep(recipe, 3, "세 번째"));
        entityManager.persistAndFlush(new RecipeStep(recipe, 1, "첫 번째"));
        entityManager.persistAndFlush(new RecipeStep(recipe, 2, "두 번째"));

        List<RecipeStep> steps = recipeStepRepository.findByRecipeIdOrderByStepOrderAsc(recipe.getId());

        assertThat(steps).extracting(RecipeStep::getStepOrder).containsExactly(1, 2, 3);
        assertThat(steps).extracting(RecipeStep::getDescription)
                .containsExactly("첫 번째", "두 번째", "세 번째");
    }
}
