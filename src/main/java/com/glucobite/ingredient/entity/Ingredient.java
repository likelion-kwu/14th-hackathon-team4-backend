package com.glucobite.ingredient.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Entity
@Table(
        name = "ingredients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ingredients_normalized_title",
                columnNames = "normalized_title"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "normalized_title", nullable = false, length = 100)
    private String normalizedTitle;

    public Ingredient(String title) {
        this.title = normalizeDisplayTitle(title);
        this.normalizedTitle = normalizeTitle(title);
    }

    public static String normalizeTitle(String title) {
        return normalizeDisplayTitle(title).toLowerCase(Locale.ROOT);
    }

    public static String normalizeDisplayTitle(String title) {
        return title.trim().replaceAll("\\s+", " ");
    }
}
