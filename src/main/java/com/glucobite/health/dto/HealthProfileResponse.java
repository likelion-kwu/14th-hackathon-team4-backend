package com.glucobite.health.dto;

import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Schema(description = "내 건강 프로필 응답")
public record HealthProfileResponse(
        @Schema(description = "생년월일", example = "2000-01-01")
        LocalDate birthDate,

        @Schema(description = "키(cm)", example = "165.50")
        BigDecimal height,

        @Schema(description = "몸무게(kg)", example = "55.20")
        BigDecimal weight,

        @Schema(description = "성별", example = "FEMALE")
        Sex sex,

        @Schema(description = "식사 건강 목표", example = "CARB_MANAGEMENT")
        HealthGoal healthGoal,

        @Schema(description = "하루 목표 탄수화물(g)", example = "180")
        Integer dailyCarbsTarget,

        @Schema(description = "혈당 측정기 연동 여부", example = "true")
        boolean glucoseDeviceConnected,

        @Schema(description = "채식 유형", example = "LACTO_OVO")
        VegetarianType vegetarianType,

        @ArraySchema(schema = @Schema(implementation = AllergenResponse.class))
        List<AllergenResponse> allergens,

        @Schema(description = "기타 식이 제한 사항", example = "갑각류 제외", nullable = true)
        String dietaryRestrictionNote,

        @Schema(description = "최근 수정 시각", example = "2026-08-20T10:30:00")
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
