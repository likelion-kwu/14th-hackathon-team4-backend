package com.glucobite.health.dto;

import com.glucobite.health.entity.Allergen;

public record AllergenResponse(
        Long allergenId,
        String name
) {

    public static AllergenResponse from(Allergen allergen) {
        return new AllergenResponse(allergen.getId(), allergen.getName());
    }
}
