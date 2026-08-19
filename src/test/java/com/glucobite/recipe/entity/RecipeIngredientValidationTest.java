package com.glucobite.recipe.entity;

import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.user.entity.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeIngredientValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;
    private static Recipe recipe;
    private static Ingredient ingredient;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
        User user = new User("amount-user", "encoded-password", "수량 사용자");
        recipe = new Recipe(user, "두부구이", null, 15);
        ingredient = new Ingredient("두부");
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void rejectsMissingZeroAndNegativeAmount() {
        RecipeIngredient missingAmount = new RecipeIngredient(recipe, ingredient, null);
        RecipeIngredient zeroAmount = new RecipeIngredient(recipe, ingredient, BigDecimal.ZERO);
        RecipeIngredient negativeAmount = new RecipeIngredient(
                recipe,
                ingredient,
                new BigDecimal("-0.01")
        );

        assertFalse(validator.validate(missingAmount).isEmpty());
        assertFalse(validator.validate(zeroAmount).isEmpty());
        assertFalse(validator.validate(negativeAmount).isEmpty());
    }

    @Test
    void acceptsPositiveAmount() {
        RecipeIngredient recipeIngredient = new RecipeIngredient(
                recipe,
                ingredient,
                new BigDecimal("0.01")
        );

        assertTrue(validator.validate(recipeIngredient).isEmpty());
    }
}
