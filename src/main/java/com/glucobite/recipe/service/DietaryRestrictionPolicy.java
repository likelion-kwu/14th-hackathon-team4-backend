package com.glucobite.recipe.service;

import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.VegetarianType;
import com.glucobite.ingredient.entity.Ingredient;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class DietaryRestrictionPolicy {

    private static final Map<String, Set<String>> ALLERGEN_ALIASES = Map.ofEntries(
            Map.entry("난류", Set.of("난류", "계란", "달걀", "메추리알")),
            Map.entry("우유", Set.of("우유", "유제품", "치즈", "버터", "요거트", "크림")),
            Map.entry("메밀", Set.of("메밀")),
            Map.entry("땅콩", Set.of("땅콩")),
            Map.entry("대두", Set.of("대두", "콩", "두부", "두유", "된장", "간장")),
            Map.entry("밀", Set.of("밀", "소맥", "wheat")),
            Map.entry("고등어", Set.of("고등어")),
            Map.entry("게", Set.of("게", "게살", "꽃게", "대게", "크랩")),
            Map.entry("새우", Set.of("새우")),
            Map.entry("돼지고기", Set.of("돼지", "돈육", "베이컨", "햄")),
            Map.entry("복숭아", Set.of("복숭아")),
            Map.entry("토마토", Set.of("토마토")),
            Map.entry("아황산류", Set.of("아황산", "sulfite")),
            Map.entry("호두", Set.of("호두")),
            Map.entry("닭고기", Set.of("닭", "치킨")),
            Map.entry("쇠고기", Set.of("쇠고기", "소고기", "우육")),
            Map.entry("오징어", Set.of("오징어")),
            Map.entry("조개류", Set.of("조개", "굴", "홍합", "전복")),
            Map.entry("잣", Set.of("잣"))
    );

    public Set<String> restrictedTerms(HealthProfile profile) {
        Set<String> terms = new HashSet<>();
        for (Allergen allergen : profile.getAllergens()) {
            terms.addAll(ALLERGEN_ALIASES.getOrDefault(
                    allergen.getName(),
                    Set.of(allergen.getName())
            ));
        }
        terms.addAll(vegetarianRestrictedTerms(profile.getVegetarianType()));
        return Set.copyOf(terms);
    }

    public boolean isRestricted(Ingredient ingredient, Set<String> restrictedTerms) {
        return isRestricted(ingredient.getTitle(), restrictedTerms);
    }

    public boolean isRestricted(String ingredientTitle, Set<String> restrictedTerms) {
        String title = normalize(ingredientTitle);
        return restrictedTerms.stream()
                .map(this::normalize)
                .anyMatch(term -> containsRestrictedTerm(title, term));
    }

    private boolean containsRestrictedTerm(String title, String term) {
        if ("밀".equals(term)) {
            return title.replace("메밀", "").contains(term);
        }
        return title.contains(term);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private Set<String> vegetarianRestrictedTerms(VegetarianType vegetarianType) {
        if (vegetarianType == null || vegetarianType == VegetarianType.NONE) {
            return Set.of();
        }
        Set<String> meatAndSeafood = Set.of(
                "돼지", "돈육", "베이컨", "햄", "소고기", "쇠고기", "우육", "닭", "치킨",
                "오리", "양고기", "생선", "고등어", "연어", "참치", "새우", "게", "오징어",
                "조개", "굴", "홍합", "전복"
        );
        if (vegetarianType == VegetarianType.PESCATARIAN) {
            return Set.of(
                    "돼지", "돈육", "베이컨", "햄", "소고기", "쇠고기", "우육",
                    "닭", "치킨", "오리", "양고기"
            );
        }
        Set<String> restricted = new HashSet<>(meatAndSeafood);
        if (vegetarianType == VegetarianType.VEGAN || vegetarianType == VegetarianType.OVO) {
            restricted.addAll(Set.of("우유", "유제품", "치즈", "버터", "요거트", "크림"));
        }
        if (vegetarianType == VegetarianType.VEGAN || vegetarianType == VegetarianType.LACTO) {
            restricted.addAll(Set.of("달걀", "계란", "난류", "메추리알"));
        }
        if (vegetarianType == VegetarianType.VEGAN) {
            restricted.add("꿀");
        }
        return restricted;
    }
}
