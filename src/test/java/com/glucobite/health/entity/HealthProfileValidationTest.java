package com.glucobite.health.entity;

import com.glucobite.user.entity.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthProfileValidationTest {

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
    void acceptsConfirmedSignupProfile() {
        assertTrue(validator.validate(validProfile()).isEmpty());
    }

    @Test
    void rejectsMissingSexAndHealthGoal() {
        HealthProfile profile = createProfile(
                LocalDate.of(2000, 1, 1),
                new BigDecimal("165.50"),
                new BigDecimal("55.20"),
                null,
                null,
                180,
                VegetarianType.NONE,
                null
        );

        Set<ConstraintViolation<HealthProfile>> violations = validator.validate(profile);

        assertTrue(hasViolation(violations, "sex"));
        assertTrue(hasViolation(violations, "healthGoal"));
    }

    @Test
    void rejectsFutureBirthDate() {
        HealthProfile profile = createProfile(
                LocalDate.now().plusDays(1),
                new BigDecimal("165.50"),
                new BigDecimal("55.20"),
                Sex.FEMALE,
                HealthGoal.CARB_MANAGEMENT,
                180,
                VegetarianType.NONE,
                null
        );

        assertTrue(hasViolation(validator.validate(profile), "birthDate"));
    }

    @Test
    void rejectsNonPositiveBodyAndCarbValues() {
        HealthProfile profile = createProfile(
                LocalDate.of(2000, 1, 1),
                BigDecimal.ZERO,
                new BigDecimal("-0.01"),
                Sex.FEMALE,
                HealthGoal.CARB_MANAGEMENT,
                0,
                VegetarianType.NONE,
                null
        );

        Set<ConstraintViolation<HealthProfile>> violations = validator.validate(profile);

        assertTrue(hasViolation(violations, "height"));
        assertTrue(hasViolation(violations, "weight"));
        assertTrue(hasViolation(violations, "dailyCarbsTarget"));
    }

    @Test
    void rejectsDietaryRestrictionOverMaximumLength() {
        HealthProfile profile = createProfile(
                LocalDate.of(2000, 1, 1),
                new BigDecimal("165.50"),
                new BigDecimal("55.20"),
                Sex.FEMALE,
                HealthGoal.CARB_MANAGEMENT,
                180,
                VegetarianType.OTHER,
                "가".repeat(501)
        );

        assertFalse(validator.validate(profile).isEmpty());
        assertTrue(hasViolation(validator.validate(profile), "dietaryRestrictionNote"));
    }

    private HealthProfile validProfile() {
        return createProfile(
                LocalDate.of(2000, 1, 1),
                new BigDecimal("165.50"),
                new BigDecimal("55.20"),
                Sex.FEMALE,
                HealthGoal.CARB_MANAGEMENT,
                180,
                VegetarianType.NONE,
                null
        );
    }

    private HealthProfile createProfile(
            LocalDate birthDate,
            BigDecimal height,
            BigDecimal weight,
            Sex sex,
            HealthGoal healthGoal,
            Integer dailyCarbsTarget,
            VegetarianType vegetarianType,
            String dietaryRestrictionNote
    ) {
        User user = new User("health-user", "encoded-password", "건강 사용자");
        return new HealthProfile(
                user,
                birthDate,
                height,
                weight,
                sex,
                healthGoal,
                null,
                false,
                dailyCarbsTarget,
                vegetarianType,
                dietaryRestrictionNote
        );
    }

    private boolean hasViolation(
            Set<ConstraintViolation<HealthProfile>> violations,
            String propertyName
    ) {
        return violations.stream()
                .anyMatch(violation -> propertyName.equals(violation.getPropertyPath().toString()));
    }
}
