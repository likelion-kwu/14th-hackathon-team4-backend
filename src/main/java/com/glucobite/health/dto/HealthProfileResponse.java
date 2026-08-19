package com.glucobite.health.dto;

import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record HealthProfileResponse(
        LocalDate birthDate,
        BigDecimal height,
        BigDecimal weight,
        Sex sex,
        HealthGoal healthGoal,
        Integer dailyCarbsTarget,
        boolean glucoseDeviceConnected,
        VegetarianType vegetarianType,
        List<AllergenResponse> allergens,
        String dietaryRestrictionNote,
        LocalDateTime updatedAt
) {

    public static HealthProfileResponse from(HealthProfile profile) {
        List<AllergenResponse> allergens = profile.getAllergens().stream()
                .sorted(Comparator.comparing(Allergen::getId))
                .map(AllergenResponse::from)
                .toList();

        return new HealthProfileResponse(
                profile.getBirthDate(),
                profile.getHeight(),
                profile.getWeight(),
                profile.getSex(),
                profile.getHealthGoal(),
                profile.getDailyCarbsTarget(),
                profile.isGlucoseDeviceConnected(),
                profile.getVegetarianType(),
                allergens,
                profile.getDietaryRestrictionNote(),
                profile.getUpdatedAt()
        );
    }
}
