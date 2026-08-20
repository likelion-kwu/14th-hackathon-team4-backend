package com.glucobite.recipe.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "사용자 입력 기반 재료 대체 후보")
public record IngredientAlternativeSuggestionResponse(
        @Schema(description = "AI 후보 ID. AI_WEB_SEARCH 후보 선택 시 preview/save에 전달")
        Long suggestionId,

        @Schema(description = "대체 Ingredient ID. REGISTERED 후보 선택 시 preview/save에 전달")
        Long substituteIngredientId,

        IngredientAlternativeSuggestionOrigin origin,

        @Schema(description = "대체 재료명", example = "단단한 두부")
        String title,

        @Schema(description = "권장 사용량(g)", example = "180.00")
        BigDecimal recommendedAmount,

        NutritionSummary nutritionChanges,

        @Schema(description = "대체 근거")
        String reason,

        @Schema(description = "조리상 주의점. 없으면 null")
        String warning,

        List<IngredientAlternativeSourceResponse> sources
) {
}
