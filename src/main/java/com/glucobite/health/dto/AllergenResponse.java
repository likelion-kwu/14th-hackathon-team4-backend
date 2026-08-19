package com.glucobite.health.dto;

import com.glucobite.health.entity.Allergen;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가입 화면에서 선택할 알레르기 항목")
public record AllergenResponse(
        @Schema(description = "알레르기 식별자", example = "2")
        Long allergenId,

        @Schema(description = "알레르기 이름", example = "우유")
        String name
) {

    public static AllergenResponse from(Allergen allergen) {
        return new AllergenResponse(allergen.getId(), allergen.getName());
    }
}
