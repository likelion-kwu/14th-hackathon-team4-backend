package com.glucobite.health.dto;

import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Schema(description = "건강 프로필 전체 수정 요청")
public record HealthProfileUpdateRequest(
        @Schema(description = "생년월일, 오늘 또는 과거 날짜", example = "2000-01-01")
        @NotNull @PastOrPresent LocalDate birthDate,

        @Schema(description = "키(cm)", example = "166.00")
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal height,

        @Schema(description = "몸무게(kg)", example = "54.80")
        @NotNull @Positive @Digits(integer = 3, fraction = 2) BigDecimal weight,

        @Schema(description = "성별", example = "FEMALE", allowableValues = {"MALE", "FEMALE"})
        @NotNull Sex sex,

        @Schema(description = "식사 건강 목표", example = "WEIGHT_MANAGEMENT")
        @NotNull HealthGoal healthGoal,

        @Schema(description = "하루 목표 탄수화물(g)", example = "170")
        @NotNull @Positive Integer dailyCarbsTarget,

        @Schema(description = "혈당 측정기 연동 여부", example = "false")
        @NotNull Boolean glucoseDeviceConnected,

        @Schema(description = "채식 유형", example = "PESCATARIAN")
        @NotNull VegetarianType vegetarianType,

        @Schema(description = "선택한 알레르기 ID 목록", example = "[1, 2, 4]")
        @NotNull Set<@NotNull @Positive Long> allergenIds,

        @Schema(description = "기타 식이 제한 사항, 공백이면 null로 초기화", example = "갑각류 제외", nullable = true)
        @Size(max = 500) String dietaryRestrictionNote
) {
}
