package com.glucobite.auth.dto;

import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record SignupProfileRequest(
        @NotNull @Past LocalDate birthDate,
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal height,
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal weight,
        @NotNull Sex sex,
        @NotNull HealthGoal healthGoal,
        @NotNull @Positive Integer dailyCarbsTarget,
        @NotNull Boolean glucoseDeviceConnected,
        @NotNull VegetarianType vegetarianType,
        @NotNull Set<@Positive Long> allergenIds,
        @Size(max = 500) String dietaryRestrictionNote
) {
}
