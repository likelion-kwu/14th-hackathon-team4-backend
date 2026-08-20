package com.glucobite.ingredient.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "ingredient_nutritions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ingredient_nutritions_ingredient_id",
                        columnNames = "ingredient_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientNutrition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_nutrition_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @PositiveOrZero
    @Column(precision = 14, scale = 6)
    private BigDecimal calories;

    @PositiveOrZero
    @Column(precision = 14, scale = 6)
    private BigDecimal carb;

    @PositiveOrZero
    @Column(precision = 14, scale = 6)
    private BigDecimal protein;

    @PositiveOrZero
    @Column(precision = 14, scale = 6)
    private BigDecimal fat;

    @PositiveOrZero
    @Column(precision = 14, scale = 6)
    private BigDecimal fiber;

    @PositiveOrZero
    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal sugar;

    @PositiveOrZero
    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal sodium;

    public IngredientNutrition(
            Ingredient ingredient,
            BigDecimal calories,
            BigDecimal carb,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal fiber
    ) {
        this(ingredient, calories, carb, protein, fat, fiber, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public IngredientNutrition(
            Ingredient ingredient,
            BigDecimal calories,
            BigDecimal carb,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal fiber,
            BigDecimal sugar,
            BigDecimal sodium
    ) {
        this.ingredient = ingredient;
        this.calories = calories;
        this.carb = carb;
        this.protein = protein;
        this.fat = fat;
        this.fiber = fiber;
        this.sugar = sugar;
        this.sodium = sodium;
    }
}
