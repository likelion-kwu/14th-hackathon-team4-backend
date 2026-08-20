package com.glucobite.recipe.substitution;

import com.glucobite.recipe.dto.NutritionSummary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIRecipeSubstitutionSuggestionGeneratorTest {

    @Test
    void convertsPerHundredGramNutritionToPerGramWithoutLosingPrecision() {
        OpenAIRecipeSubstitutionSuggestionGenerator.NutritionOutput nutrition =
                new OpenAIRecipeSubstitutionSuggestionGenerator.NutritionOutput();
        nutrition.calories = 76.0;
        nutrition.carb = 1.9;
        nutrition.protein = 8.1;
        nutrition.fat = 4.2;
        nutrition.fiber = 0.3;
        nutrition.sugar = 0.7;
        nutrition.sodium = 7.0;

        OpenAIRecipeSubstitutionSuggestionGenerator.IngredientOutput ingredient =
                new OpenAIRecipeSubstitutionSuggestionGenerator.IngredientOutput();
        ingredient.title = "단단한 두부";
        ingredient.recommendedAmount = 180.0;
        ingredient.reason = "단백질을 유지합니다.";
        ingredient.warning = "수분을 제거합니다.";
        ingredient.nutritionPer100g = nutrition;

        OpenAIRecipeSubstitutionSuggestionGenerator.SuggestionOutput output =
                new OpenAIRecipeSubstitutionSuggestionGenerator.SuggestionOutput();
        output.suggestions = List.of(ingredient);

        GeneratedSubstitutionSuggestions generated = output.toGenerated(
                "resp_123",
                List.of(new GeneratedSubstitutionSuggestions.Source(
                        "공식 영양정보",
                        "https://example.com/nutrition"
                ))
        );

        NutritionSummary perGram = generated.suggestions().getFirst().nutritionPerGram();
        assertThat(perGram.calories()).isEqualByComparingTo(new BigDecimal("0.760000"));
        assertThat(perGram.carb()).isEqualByComparingTo(new BigDecimal("0.019000"));
        assertThat(perGram.fiber()).isEqualByComparingTo(new BigDecimal("0.003000"));
        assertThat(perGram.sodium()).isEqualByComparingTo(new BigDecimal("0.070000"));
        assertThat(generated.sources()).hasSize(1);
    }

    @Test
    void keepsMissingOutputFieldsNullForServiceValidation() {
        OpenAIRecipeSubstitutionSuggestionGenerator.IngredientOutput ingredient =
                new OpenAIRecipeSubstitutionSuggestionGenerator.IngredientOutput();
        ingredient.title = "두부";

        OpenAIRecipeSubstitutionSuggestionGenerator.SuggestionOutput output =
                new OpenAIRecipeSubstitutionSuggestionGenerator.SuggestionOutput();
        output.suggestions = List.of(ingredient);

        GeneratedSubstitutionSuggestions.Suggestion suggestion = output.toGenerated(
                "resp_missing",
                List.of()
        ).suggestions().getFirst();

        assertThat(suggestion.recommendedAmount()).isNull();
        assertThat(suggestion.nutritionPerGram()).isNull();
    }
}
