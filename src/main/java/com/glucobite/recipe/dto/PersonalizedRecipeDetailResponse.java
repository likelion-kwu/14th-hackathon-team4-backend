package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "GPT가 생성하고 저장한 개인화 후보 응답")
public record PersonalizedRecipeDetailResponse(
        @Schema(description = "저장된 개인화 후보 Recipe ID", example = "12")
        Long candidateRecipeId,

        @Schema(description = "원본 레시피 ID", example = "1")
        Long originalRecipeId,

        @Schema(description = "후보 요약 라벨", example = "고단백질 위주 수정안")
        String label,

        @Schema(description = "건강 목표와 변경 이유")
        String reason,

        @Schema(description = "제목", example = "개인화 닭가슴살 볶음밥")
        String title,

        @Schema(description = "설명", example = "사용자의 탄수화물 목표에 맞게 조정된 레시피입니다.")
        String description,

        @Schema(description = "조리 시간(분)", example = "20")
        Integer cookingTime,

        @Schema(description = "기존(원본) 영양 정보")
        NutritionSummary originalNutrition,

        @Schema(description = "GPT 개인화 후보 영양 정보")
        NutritionSummary personalizedNutrition,

        @Schema(description = "기존 대비 영양 변화량 (음수 가능)")
        NutritionSummary nutritionChanges,

        @Schema(description = "원본 재료 목록")
        List<RecipeIngredientResponse> originalIngredients,

        @Schema(description = "재료 목록. 각 항목에 changed/changeReason 포함")
        List<PersonalizedIngredientResponse> ingredients,

        @Schema(description = "조리 단계 (step_order ASC)")
        List<RecipeStepResponse> steps
) {
}
