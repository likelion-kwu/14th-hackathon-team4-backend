package com.glucobite.recipe.service;

import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.recipe.dto.NutritionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeNutritionCalculatorTest {

    private RecipeNutritionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RecipeNutritionCalculator();
    }

    @Test
    void multipliesNutritionByAmount() {
        Ingredient ingredient = new Ingredient("두부");
        IngredientNutrition nutrition = new IngredientNutrition(
                ingredient,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("8.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00")
        );

        NutritionSummary result = calculator.contribute(nutrition, new BigDecimal("2"));

        assertThat(result.calories()).isEqualByComparingTo("200.00");
        assertThat(result.carb()).isEqualByComparingTo("20.00");
        assertThat(result.protein()).isEqualByComparingTo("16.00");
        assertThat(result.fat()).isEqualByComparingTo("10.00");
        assertThat(result.fiber()).isEqualByComparingTo("4.00");
    }

    @Test
    void includesSugarAndSodiumInContribution() {
        IngredientNutrition nutrition = new IngredientNutrition(
                new Ingredient("간장 양념"),
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                new BigDecimal("1.00"),
                new BigDecimal("4.00"),
                new BigDecimal("300.00")
        );

        NutritionSummary result = calculator.contribute(nutrition, new BigDecimal("0.50"));

        assertThat(result.sugar()).isEqualByComparingTo("2.0000");
        assertThat(result.sodium()).isEqualByComparingTo("150.0000");
    }

    @Test
    void returnsZeroWhenNutritionIsMissing() {
        NutritionSummary result = calculator.contribute(null, new BigDecimal("1.5"));

        assertThat(result.calories()).isEqualByComparingTo("0");
        assertThat(result.carb()).isEqualByComparingTo("0");
    }

    @Test
    void treatsNullNutritionFieldsAsZero() {
        Ingredient ingredient = new Ingredient("두부");
        IngredientNutrition nutrition = new IngredientNutrition(ingredient, null, null, null, null, null);

        NutritionSummary result = calculator.contribute(nutrition, new BigDecimal("3"));

        assertThat(result.calories()).isEqualByComparingTo("0");
        assertThat(result.fiber()).isEqualByComparingTo("0");
    }

    @Test
    void sumsMultipleSummaries() {
        NutritionSummary first = new NutritionSummary(
                new BigDecimal("100"),
                new BigDecimal("10"),
                new BigDecimal("8"),
                new BigDecimal("5"),
                new BigDecimal("2")
        );
        NutritionSummary second = new NutritionSummary(
                new BigDecimal("50"),
                new BigDecimal("5"),
                new BigDecimal("4"),
                new BigDecimal("2"),
                new BigDecimal("1")
        );

        NutritionSummary result = calculator.sum(List.of(first, second));

        assertThat(result.calories()).isEqualByComparingTo("150");
        assertThat(result.carb()).isEqualByComparingTo("15");
        assertThat(result.protein()).isEqualByComparingTo("12");
        assertThat(result.fat()).isEqualByComparingTo("7");
        assertThat(result.fiber()).isEqualByComparingTo("3");
    }

    @Test
    void computesChangesAsPersonalizedMinusBase() {
        NutritionSummary base = new NutritionSummary(
                new BigDecimal("200"),
                new BigDecimal("30"),
                new BigDecimal("15"),
                new BigDecimal("10"),
                new BigDecimal("5")
        );
        NutritionSummary personalized = new NutritionSummary(
                new BigDecimal("150"),
                new BigDecimal("20"),
                new BigDecimal("18"),
                new BigDecimal("6"),
                new BigDecimal("6")
        );

        NutritionSummary changes = calculator.changes(base, personalized);

        assertThat(changes.calories()).isEqualByComparingTo("-50");
        assertThat(changes.carb()).isEqualByComparingTo("-10");
        assertThat(changes.protein()).isEqualByComparingTo("3");
        assertThat(changes.fat()).isEqualByComparingTo("-4");
        assertThat(changes.fiber()).isEqualByComparingTo("1");
    }
}
