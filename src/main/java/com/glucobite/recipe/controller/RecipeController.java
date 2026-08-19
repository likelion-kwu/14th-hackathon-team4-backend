package com.glucobite.recipe.controller;

import com.glucobite.recipe.dto.RecipePageResponse;
import com.glucobite.recipe.service.RecipeService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    public RecipePageResponse getRecipes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        return recipeService.findRecipes(userId, completed, page, size);
    }
}
