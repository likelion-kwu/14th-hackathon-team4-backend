package com.glucobite.recipe.dto;

import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.entity.RecipeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "분석 후 저장된 기본 Recipe")
public record ImportedRecipeResponse(
        @Schema(description = "저장된 Recipe ID", example = "1")
        Long recipeId,
        @Schema(description = "레시피 제목", example = "계란 볶음밥")
        String title,
        @Schema(description = "레시피 설명", example = "간단하게 만드는 계란 볶음밥")
        String description,
        @Schema(description = "조리 시간(분)", example = "20")
        Integer cookingTime,
        @Schema(description = "불러오기 유형", example = "TEXT")
        RecipeImportType importType,
        @Schema(description = "Recipe 단계 유형", example = "BASE")
        RecipeType recipeType,
        @Schema(description = "개인화 완료 여부. 기본 Recipe는 false", example = "false")
        boolean completed,
        @Schema(description = "원본 URL", example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        String sourceUrl,
        @Schema(description = "외부 서비스의 원본 식별자", example = "dQw4w9WgXcQ")
        String sourceExternalId,
        @Schema(description = "원본 대표 이미지 URL")
        String imageUrl,
        @Schema(description = "전체 영양정보")
        NutritionSummary nutrition,
        @Schema(description = "추출된 재료")
        List<RecipeIngredientResponse> ingredients,
        @Schema(description = "추출된 조리 단계")
        List<RecipeStepResponse> steps
) {
}
