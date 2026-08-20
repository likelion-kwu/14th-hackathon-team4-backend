package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "개인화 레시피 상세 조회 응답 (Stateless)")
public record PersonalizedRecipeDetailResponse(
        @Schema(description = "원본 레시피 ID", example = "1")
        Long originalRecipeId,

        @Schema(description = "제목", example = "개인화 닭가슴살 볶음밥")
        String title,

        @Schema(description = "설명", example = "사용자의 탄수화물 목표에 맞게 조정된 레시피입니다.")
        String description,

        @Schema(description = "조리 시간(분)", example = "20")
        Integer cookingTime,

        @Schema(description = "기존(원본) 영양 정보")
        NutritionSummary originalNutrition,

        @Schema(description = "개인화 이후 영양 정보. 저장 없이 계산되며 자동 대체 미적용 시 원본과 동일")
        NutritionSummary personalizedNutrition,

        @Schema(description = "기존 대비 영양 변화량 (음수 가능)")
        NutritionSummary nutritionChanges,

        @Schema(description = "재료 목록. 각 항목에 changed/changeReason 포함")
        List<PersonalizedIngredientResponse> ingredients,

        @Schema(description = "조리 단계 (step_order ASC)")
        List<RecipeStepResponse> steps
) {
}
