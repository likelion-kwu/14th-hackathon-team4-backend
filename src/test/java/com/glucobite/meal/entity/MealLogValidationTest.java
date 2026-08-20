package com.glucobite.meal.entity;

import com.glucobite.user.entity.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealLogValidationTest {

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
    void rejectsNegativeHealthMetrics() {
        User user = new User("meal-user", "encoded-password", "식사 사용자");
        BigDecimal negative = new BigDecimal("-0.01");
        BigDecimal zero = BigDecimal.ZERO;

        List<MealLog> invalidMealLogs = List.of(
                createMealLog(user, negative, zero, zero),
                createMealLog(user, zero, negative, zero),
                createMealLog(user, zero, zero, negative)
        );

        invalidMealLogs.forEach(mealLog ->
                assertFalse(validator.validate(mealLog).isEmpty())
        );
    }

    @Test
    void acceptsZeroNutritionMetrics() {
        User user = new User("meal-user", "encoded-password", "식사 사용자");
        MealLog zeroMealLog = createMealLog(
                user,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        assertTrue(validator.validate(zeroMealLog).isEmpty());
    }

    private MealLog createMealLog(
            User user,
            BigDecimal calories,
            BigDecimal carb,
            BigDecimal sugar
    ) {
        return new MealLog(
                user,
                null,
                "직접 식사",
                null,
                MealType.BREAKFAST,
                calories,
                carb,
                sugar,
                LocalDateTime.of(2026, 8, 19, 8, 0)
        );
    }
}
