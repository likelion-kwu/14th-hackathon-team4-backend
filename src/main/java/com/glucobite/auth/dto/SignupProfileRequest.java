package com.glucobite.auth.dto;

import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Schema(description = "가입 건강 프로필 요청")
public record SignupProfileRequest(
        @Schema(description = "생년월일, 오늘 또는 과거 날짜", example = "2000-01-01")
        @NotNull @PastOrPresent LocalDate birthDate,

        @Schema(description = "키(cm)", example = "165.50")
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal height,

        @Schema(description = "몸무게(kg)", example = "55.20")
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal weight,

        @Schema(description = "성별", example = "FEMALE", allowableValues = {"MALE", "FEMALE"})
        @NotNull Sex sex,

        @Schema(description = "식사 건강 목표", example = "CARB_MANAGEMENT")
        @NotNull HealthGoal healthGoal,

        @Schema(description = "하루 목표 탄수화물(g)", example = "180")
        @NotNull @Positive Integer dailyCarbsTarget,

        @Schema(description = "혈당 측정기 연동 여부", example = "true")
        @NotNull Boolean glucoseDeviceConnected,

        @Schema(description = "채식 유형", example = "LACTO_OVO")
        @NotNull VegetarianType vegetarianType,

        @Schema(description = "선택한 알레르기 항목 ID 목록", example = "[1, 2]")
        @NotNull Set<@Positive Long> allergenIds,

        @Schema(description = "기타 식이 제한 사항, 최대 500자", example = "갑각류 제외")
        @Size(max = 500) String dietaryRestrictionNote
) {
}
