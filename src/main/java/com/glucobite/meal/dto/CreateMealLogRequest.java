package com.glucobite.meal.dto;

import com.glucobite.meal.entity.MealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import com.glucobite.common.validation.PastOrPresentKst;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "식사 기록 생성 요청")
public record CreateMealLogRequest(
        Long recipeId,
        @Size(max = 150) String title,
        @Size(max = 500) String imageUrl,
        @NotNull MealType mealType,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal calories,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal carb,
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal sugar,
        @NotNull @PastOrPresentKst LocalDateTime eatenAt
) {
    @AssertTrue(message = "Recipe 또는 직접 입력 영양값 중 하나만 사용해야 합니다.")
    @Schema(hidden = true)
    public boolean isSourceValid() {
        if (recipeId != null) {
            return isBlank(title) && calories == null && carb == null && sugar == null;
        }
        return !isBlank(title) && calories != null && carb != null && sugar != null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
