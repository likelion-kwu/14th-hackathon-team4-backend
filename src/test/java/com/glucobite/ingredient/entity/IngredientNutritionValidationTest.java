package com.glucobite.ingredient.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientNutritionValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void rejectsNegativeNutritionValues() {
        Ingredient ingredient = new Ingredient("두부");
        BigDecimal negative = new BigDecimal("-0.01");
        BigDecimal zero = BigDecimal.ZERO;

        List<IngredientNutrition> invalidNutritions = List.of(
                new IngredientNutrition(ingredient, negative, zero, zero, zero, zero),
                new IngredientNutrition(ingredient, zero, negative, zero, zero, zero),
                new IngredientNutrition(ingredient, zero, zero, negative, zero, zero),
                new IngredientNutrition(ingredient, zero, zero, zero, negative, zero),
                new IngredientNutrition(ingredient, zero, zero, zero, zero, negative)
        );

        invalidNutritions.forEach(nutrition ->
                assertFalse(validator.validate(nutrition).isEmpty())
        );
    }

    @Test
    void acceptsZeroAndUnknownNutritionValues() {
        Ingredient ingredient = new Ingredient("두부");
        IngredientNutrition zeroNutrition = new IngredientNutrition(
                ingredient,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        IngredientNutrition unknownNutrition = new IngredientNutrition(
                ingredient,
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(validator.validate(zeroNutrition).isEmpty());
        assertTrue(validator.validate(unknownNutrition).isEmpty());
    }
}
