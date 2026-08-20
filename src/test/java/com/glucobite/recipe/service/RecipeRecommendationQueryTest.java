package com.glucobite.recipe.service;

import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.recipe.dto.RecipeRecommendationResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeRecommendationQueryTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeStepRepository recipeStepRepository;
    @Mock private RecipeIngredientRepository recipeIngredientRepository;
    @Mock private IngredientNutritionRepository ingredientNutritionRepository;
    @Mock private HealthProfileRepository healthProfileRepository;
    @Mock private DietaryRestrictionPolicy dietaryRestrictionPolicy;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(
                recipeRepository,
                recipeStepRepository,
                recipeIngredientRepository,
                ingredientNutritionRepository,
                healthProfileRepository,
                new RecipeNutritionCalculator(),
                dietaryRestrictionPolicy
        );
    }

    @Test
    void loadsIngredientsForAllRecommendationCandidatesInOneRepositoryCall() {
        HealthProfile profile = mock(HealthProfile.class);
        when(profile.getDailyCarbsTarget()).thenReturn(180);
        when(healthProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(dietaryRestrictionPolicy.restrictedTerms(profile)).thenReturn(Set.of());
        Recipe first = recipe(1L, "첫 번째");
        Recipe second = recipe(2L, "두 번째");
        when(recipeRepository.findByUserIdOrderByCreatedAtDescIdDesc(7L))
                .thenReturn(List.of(first, second));
        when(recipeIngredientRepository.findByRecipeIdIn(List.of(1L, 2L)))
                .thenReturn(List.of());

        RecipeRecommendationResponse response = recipeService.getRecommendations(7L);

        assertThat(response.recommendations()).hasSize(2);
        verify(recipeIngredientRepository).findByRecipeIdIn(List.of(1L, 2L));
        verify(recipeIngredientRepository, never()).findByRecipeId(anyLong());
    }

    @Test
    void skipsBulkIngredientQueryWhenUserHasNoRecipes() {
        HealthProfile profile = mock(HealthProfile.class);
        when(healthProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));
        when(dietaryRestrictionPolicy.restrictedTerms(profile)).thenReturn(Set.of());
        when(recipeRepository.findByUserIdOrderByCreatedAtDescIdDesc(7L)).thenReturn(List.of());

        RecipeRecommendationResponse response = recipeService.getRecommendations(7L);

        assertThat(response.recommendations()).isEmpty();
        verify(recipeIngredientRepository, never()).findByRecipeIdIn(anyCollection());
    }

    private Recipe recipe(Long id, String title) {
        Recipe recipe = mock(Recipe.class);
        when(recipe.getId()).thenReturn(id);
        when(recipe.getTitle()).thenReturn(title);
        return recipe;
    }
}
