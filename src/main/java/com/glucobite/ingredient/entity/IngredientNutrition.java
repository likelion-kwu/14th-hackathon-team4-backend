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
    @Column(precision = 10, scale = 2)
    private BigDecimal calories;

    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal carb;

    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal protein;

    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal fat;

    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal fiber;

    public IngredientNutrition(
            Ingredient ingredient,
            BigDecimal calories,
            BigDecimal carb,
            BigDecimal protein,
            BigDecimal fat,
            BigDecimal fiber
    ) {
        this.ingredient = ingredient;
        this.calories = calories;
        this.carb = carb;
        this.protein = protein;
        this.fat = fat;
        this.fiber = fiber;
    }
}
