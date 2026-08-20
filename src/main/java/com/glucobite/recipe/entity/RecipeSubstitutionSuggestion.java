package com.glucobite.recipe.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "recipe_substitution_suggestions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recipe_sub_suggestion_request_order",
                columnNames = {
                        "user_id",
                        "recipe_id",
                        "original_ingredient_id",
                        "request_key",
                        "suggestion_order"
                }
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeSubstitutionSuggestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suggestion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "original_ingredient_id", nullable = false)
    private Ingredient originalIngredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitute_ingredient_id", nullable = false)
    private Ingredient substituteIngredient;

    @NotBlank
    @Size(max = 64)
    @Column(name = "request_key", nullable = false, length = 64)
    private String requestKey;

    @NotNull
    @Positive
    @Column(name = "suggestion_order", nullable = false)
    private Integer suggestionOrder;

    @NotBlank
    @Size(max = 300)
    @Column(name = "user_input", nullable = false, length = 300)
    private String userInput;

    @NotNull
    @Positive
    @Digits(integer = 8, fraction = 2)
    @Column(name = "recommended_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal recommendedAmount;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String reason;

    @Size(max = 500)
    @Column(length = 500)
    private String warning;

    @NotNull
    @PositiveOrZero
    @Column(name = "calories_per_gram", nullable = false, precision = 14, scale = 6)
    private BigDecimal caloriesPerGram;

    @NotNull
    @PositiveOrZero
    @Column(name = "carb_per_gram", nullable = false, precision = 14, scale = 6)
    private BigDecimal carbPerGram;

    @NotNull
    @PositiveOrZero
    @Column(name = "protein_per_gram", nullable = false, precision = 14, scale = 6)
    private BigDecimal proteinPerGram;

    @NotNull
    @PositiveOrZero
    @Column(name = "fat_per_gram", nullable = false, precision = 14, scale = 6)
    private BigDecimal fatPerGram;

    @NotNull
    @PositiveOrZero
    @Column(name = "fiber_per_gram", nullable = false, precision = 14, scale = 6)
    private BigDecimal fiberPerGram;

    @NotNull
    @PositiveOrZero
    @Column(name = "sugar_per_gram", nullable = false, precision = 14, scale = 6)
    private BigDecimal sugarPerGram;

    @NotNull
    @PositiveOrZero
    @Column(name = "sodium_per_gram", nullable = false, precision = 14, scale = 6)
    private BigDecimal sodiumPerGram;

    @Size(max = 100)
    @Column(name = "openai_response_id", length = 100)
    private String openAIResponseId;

    public RecipeSubstitutionSuggestion(
            User user,
            Recipe recipe,
            Ingredient originalIngredient,
            Ingredient substituteIngredient,
            String requestKey,
            Integer suggestionOrder,
            String userInput,
            BigDecimal recommendedAmount,
            String reason,
            String warning,
            NutritionSummary nutritionPerGram,
            String openAIResponseId
    ) {
        this.user = user;
        this.recipe = recipe;
        this.originalIngredient = originalIngredient;
        this.substituteIngredient = substituteIngredient;
        this.requestKey = requestKey;
        this.suggestionOrder = suggestionOrder;
        this.userInput = userInput;
        this.recommendedAmount = recommendedAmount;
        this.reason = reason;
        this.warning = warning;
        this.caloriesPerGram = nutritionPerGram.calories();
        this.carbPerGram = nutritionPerGram.carb();
        this.proteinPerGram = nutritionPerGram.protein();
        this.fatPerGram = nutritionPerGram.fat();
        this.fiberPerGram = nutritionPerGram.fiber();
        this.sugarPerGram = nutritionPerGram.sugar();
        this.sodiumPerGram = nutritionPerGram.sodium();
        this.openAIResponseId = openAIResponseId;
    }

    public NutritionSummary nutritionPerGram() {
        return new NutritionSummary(
                caloriesPerGram,
                carbPerGram,
                proteinPerGram,
                fatPerGram,
                fiberPerGram,
                sugarPerGram,
                sodiumPerGram
        );
    }
}

