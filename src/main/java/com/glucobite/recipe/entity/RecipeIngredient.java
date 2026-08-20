package com.glucobite.recipe.entity;

import com.glucobite.ingredient.entity.Ingredient;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "recipe_ingredients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recipe_ingredient",
                        columnNames = {"recipe_id", "ingredient_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_ingredient_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @PositiveOrZero
    @Column(name = "calories_per_gram", precision = 14, scale = 6)
    private BigDecimal caloriesPerGram;

    @PositiveOrZero
    @Column(name = "carb_per_gram", precision = 14, scale = 6)
    private BigDecimal carbPerGram;

    @PositiveOrZero
    @Column(name = "protein_per_gram", precision = 14, scale = 6)
    private BigDecimal proteinPerGram;

    @PositiveOrZero
    @Column(name = "fat_per_gram", precision = 14, scale = 6)
    private BigDecimal fatPerGram;

    @PositiveOrZero
    @Column(name = "fiber_per_gram", precision = 14, scale = 6)
    private BigDecimal fiberPerGram;

    @PositiveOrZero
    @Column(name = "sugar_per_gram", precision = 14, scale = 6)
    private BigDecimal sugarPerGram;

    @PositiveOrZero
    @Column(name = "sodium_per_gram", precision = 14, scale = 6)
    private BigDecimal sodiumPerGram;

    public RecipeIngredient(
            Recipe recipe,
            Ingredient ingredient,
            BigDecimal amount
    ) {
        this(recipe, ingredient, amount, null);
    }

    public RecipeIngredient(
            Recipe recipe,
            Ingredient ingredient,
            BigDecimal amount,
            NutritionSnapshot nutritionSnapshot
    ) {
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.amount = amount;
        if (nutritionSnapshot != null) {
            this.caloriesPerGram = nutritionSnapshot.calories();
            this.carbPerGram = nutritionSnapshot.carb();
            this.proteinPerGram = nutritionSnapshot.protein();
            this.fatPerGram = nutritionSnapshot.fat();
            this.fiberPerGram = nutritionSnapshot.fiber();
            this.sugarPerGram = nutritionSnapshot.sugar();
            this.sodiumPerGram = nutritionSnapshot.sodium();
        }
    }

    public NutritionSnapshot nutritionSnapshot() {
        if (caloriesPerGram == null
                && carbPerGram == null
                && proteinPerGram == null
                && fatPerGram == null
                && fiberPerGram == null
                && sugarPerGram == null
                && sodiumPerGram == null) {
            return null;
        }
        return new NutritionSnapshot(
                caloriesPerGram,
                carbPerGram,
                proteinPerGram,
                fatPerGram,
                fiberPerGram,
                sugarPerGram,
                sodiumPerGram
        );
    }

    public record NutritionSnapshot(
            BigDecimal calories,
            BigDecimal carb,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal fiber,
            BigDecimal sugar,
            BigDecimal sodium
    ) {
    }
}
