package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "복수 재료 대체 미리보기 응답 (Stateless)")
public record RecipeSubstitutionPreviewResponse(
        @Schema(description = "원본 레시피 ID", example = "1")
        Long originalRecipeId,

        @Schema(description = "원본 레시피 전체 영양 정보")
        NutritionSummary originalNutrition,

        @Schema(description = "모든 대체 항목을 반영한 전체 영양 정보")
        NutritionSummary personalizedNutrition,

        @Schema(description = "원본 대비 전체 영양 변화량")
        NutritionSummary nutritionChanges,

        @Schema(description = "적용된 대체 재료 목록")
        List<ChangedIngredientResponse> changedIngredients,

        @Schema(description = "모든 대체 항목을 반영한 최종 재료 목록")
        List<PersonalizedIngredientResponse> ingredients
) {
}
