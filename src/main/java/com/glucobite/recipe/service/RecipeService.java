package com.glucobite.recipe.service;

import com.glucobite.recipe.dto.RecipePageResponse;
import com.glucobite.recipe.dto.RecipeSummaryResponse;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.repository.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecipeService {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public RecipePageResponse findRecipes(
            Long userId,
            Boolean completed,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, DEFAULT_SORT);
        Page<Recipe> recipes = completed == null
                ? recipeRepository.findByUserId(userId, pageable)
                : recipeRepository.findByUserIdAndCompleted(userId, completed, pageable);

        return RecipePageResponse.from(recipes.map(RecipeSummaryResponse::from));
    }
}
