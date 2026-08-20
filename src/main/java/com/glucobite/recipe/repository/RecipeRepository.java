package com.glucobite.recipe.repository;

import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.entity.RecipeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Page<Recipe> findByUserIdAndRecipeTypeIn(
            Long userId,
            List<RecipeType> recipeTypes,
            Pageable pageable
    );

    Page<Recipe> findByUserIdAndCompletedAndRecipeTypeIn(
            Long userId,
            boolean completed,
            List<RecipeType> recipeTypes,
            Pageable pageable
    );

    Optional<Recipe> findByIdAndUserId(Long id, Long userId);

    Optional<Recipe> findFirstByUserIdAndImportTypeAndSourceExternalIdOrderByIdAsc(
            Long userId,
            RecipeImportType importType,
            String sourceExternalId
    );

    List<Recipe> findByUserIdAndRecipeTypeInOrderByCreatedAtDescIdDesc(
            Long userId,
            List<RecipeType> recipeTypes
    );
}
