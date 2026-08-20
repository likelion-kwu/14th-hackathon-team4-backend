package com.glucobite.recipe.service;

import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DietaryRestrictionPolicyTest {

    private DietaryRestrictionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DietaryRestrictionPolicy();
    }

    @Test
    void matchesCommonIngredientAliasesForCanonicalAllergens() {
        HealthProfile profile = profile(VegetarianType.NONE, new Allergen("난류"));
        Set<String> restrictedTerms = policy.restrictedTerms(profile);

        assertThat(policy.isRestricted(new Ingredient("계란 2개"), restrictedTerms)).isTrue();
        assertThat(policy.isRestricted(new Ingredient("달걀 흰자"), restrictedTerms)).isTrue();
    }

    @Test
    void doesNotTreatBuckwheatAsWheat() {
        HealthProfile profile = profile(VegetarianType.NONE, new Allergen("밀"));
        Set<String> restrictedTerms = policy.restrictedTerms(profile);

        assertThat(policy.isRestricted(new Ingredient("메밀가루"), restrictedTerms)).isFalse();
        assertThat(policy.isRestricted(new Ingredient("통밀가루"), restrictedTerms)).isTrue();
    }

    @Test
    void appliesVegetarianRestrictionsWithoutMatchingAllergens() {
        HealthProfile profile = profile(VegetarianType.VEGAN);
        Set<String> restrictedTerms = policy.restrictedTerms(profile);

        assertThat(policy.isRestricted(new Ingredient("닭가슴살"), restrictedTerms)).isTrue();
        assertThat(policy.isRestricted(new Ingredient("두부"), restrictedTerms)).isFalse();
    }

    private HealthProfile profile(VegetarianType vegetarianType, Allergen... allergens) {
        return new HealthProfile(
                new User("policy-user", "encoded-password", "정책 사용자"),
                LocalDate.of(1990, 1, 1),
                new BigDecimal("170.00"),
                new BigDecimal("65.00"),
                Sex.MALE,
                HealthGoal.CARB_MANAGEMENT,
                null,
                false,
                180,
                vegetarianType,
                null,
                List.of(allergens)
        );
    }
}
