package com.glucobite.recipe.repository;

import com.glucobite.recipe.entity.RecipeIngredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    @EntityGraph(attributePaths = "ingredient")
    List<RecipeIngredient> findByRecipeId(Long recipeId);

    @EntityGraph(attributePaths = "ingredient")
    List<RecipeIngredient> findByRecipeIdIn(Collection<Long> recipeIds);

    Optional<RecipeIngredient> findByRecipeIdAndIngredientId(Long recipeId, Long ingredientId);
}
