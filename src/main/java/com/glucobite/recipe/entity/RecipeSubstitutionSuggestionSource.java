package com.glucobite.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "recipe_substitution_suggestion_sources",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recipe_sub_suggestion_source_order",
                columnNames = {"suggestion_id", "source_order"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeSubstitutionSuggestionSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suggestion_source_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggestion_id", nullable = false)
    private RecipeSubstitutionSuggestion suggestion;

    @NotNull
    @Positive
    @Column(name = "source_order", nullable = false)
    private Integer sourceOrder;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String title;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String url;

    public RecipeSubstitutionSuggestionSource(
            RecipeSubstitutionSuggestion suggestion,
            Integer sourceOrder,
            String title,
            String url
    ) {
        this.suggestion = suggestion;
        this.sourceOrder = sourceOrder;
        this.title = title;
        this.url = url;
    }
}
