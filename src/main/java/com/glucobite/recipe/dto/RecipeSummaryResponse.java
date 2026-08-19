package com.glucobite.recipe.dto;

import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeImportType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "레시피 목록 항목")
public record RecipeSummaryResponse(
        @Schema(description = "레시피 식별자", example = "15")
        Long recipeId,

        @Schema(description = "레시피 이름", example = "두부 채소 덮밥")
        String title,

        @Schema(description = "조리시간(분)", example = "20", nullable = true)
        Integer cookingTime,

        @Schema(description = "분석된 총열량(kcal)", example = "430.50", nullable = true)
        BigDecimal totalCalories,

        @Schema(description = "레시피 유입 방식", example = "URL", nullable = true)
        RecipeImportType importType,

        @Schema(description = "조리 완료 여부", example = "true")
        boolean completed,

        @Schema(description = "레시피 생성 시각", example = "2026-08-19T12:30:00")
        LocalDateTime createdAt
) {

    public static RecipeSummaryResponse from(Recipe recipe) {
        return new RecipeSummaryResponse(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getCookingTime(),
                recipe.getTotalCalories(),
                recipe.getImportType(),
                recipe.isCompleted(),
                recipe.getCreatedAt()
        );
    }
}
