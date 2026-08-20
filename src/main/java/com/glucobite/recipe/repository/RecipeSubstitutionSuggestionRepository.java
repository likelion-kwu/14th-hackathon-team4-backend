package com.glucobite.recipe.repository;

import com.glucobite.recipe.entity.RecipeSubstitutionSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeSubstitutionSuggestionRepository
        extends JpaRepository<RecipeSubstitutionSuggestion, Long> {

    List<RecipeSubstitutionSuggestion>
    findByUserIdAndRecipeIdAndOriginalIngredientIdAndRequestKeyOrderBySuggestionOrderAsc(
            Long userId,
            Long recipeId,
            Long originalIngredientId,
            String requestKey
    );

    Optional<RecipeSubstitutionSuggestion>
    findByIdAndUserIdAndRecipeIdAndOriginalIngredientId(
            Long id,
            Long userId,
            Long recipeId,
            Long originalIngredientId
    );
}
