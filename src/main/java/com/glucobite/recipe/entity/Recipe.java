package com.glucobite.recipe.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import com.glucobite.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cooking_time")
    private Integer cookingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "import_type", length = 20)
    private RecipeImportType importType;

    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "total_calories", precision = 10, scale = 2)
    private BigDecimal totalCalories;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipe_type", nullable = false, length = 40)
    private RecipeType recipeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_recipe_id")
    private Recipe sourceRecipe;

    @Column(name = "personalization_label", length = 150)
    private String personalizationLabel;

    @Column(name = "personalization_reason", length = 500)
    private String personalizationReason;

    @Column(name = "openai_response_id", length = 100)
    private String openAIResponseId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_external_id", length = 100)
    private String sourceExternalId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public Recipe(
            User user,
            String title,
            String description,
            Integer cookingTime
    ) {
        this(user, title, description, cookingTime, null, null);
    }

    public Recipe(
            User user,
            String title,
            String description,
            Integer cookingTime,
            RecipeImportType importType,
            BigDecimal totalCalories
    ) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.cookingTime = cookingTime;
        this.importType = importType;
        this.totalCalories = totalCalories;
        this.completed = false;
        this.recipeType = RecipeType.BASE;
    }

    public static Recipe personalizationCandidate(
            Recipe sourceRecipe,
            String title,
            String description,
            Integer cookingTime,
            BigDecimal totalCalories,
            String personalizationLabel,
            String personalizationReason,
            String openAIResponseId
    ) {
        Recipe candidate = new Recipe(
                sourceRecipe.getUser(),
                title,
                description,
                cookingTime,
                sourceRecipe.getImportType(),
                totalCalories
        );
        candidate.recipeType = RecipeType.PERSONALIZATION_CANDIDATE;
        candidate.sourceRecipe = sourceRecipe;
        candidate.personalizationLabel = personalizationLabel;
        candidate.personalizationReason = personalizationReason;
        candidate.openAIResponseId = openAIResponseId;
        return candidate;
    }

    public static Recipe personalizedFrom(
            Recipe workingRecipe,
            String title,
            BigDecimal totalCalories
    ) {
        Recipe personalized = new Recipe(
                workingRecipe.getUser(),
                title,
                workingRecipe.getDescription(),
                workingRecipe.getCookingTime(),
                workingRecipe.getImportType(),
                totalCalories
        );
        personalized.completed = true;
        personalized.recipeType = RecipeType.PERSONALIZED;
        personalized.sourceRecipe = workingRecipe.recipeType == RecipeType.BASE
                ? workingRecipe
                : workingRecipe.sourceRecipe;
        personalized.personalizationLabel = workingRecipe.personalizationLabel;
        personalized.personalizationReason = workingRecipe.personalizationReason;
        personalized.openAIResponseId = workingRecipe.openAIResponseId;
        return personalized;
    }

    public void complete() {
        this.completed = true;
        this.recipeType = RecipeType.PERSONALIZED;
    }

    public void attachSourceMetadata(
            String sourceUrl,
            String sourceExternalId,
            String imageUrl
    ) {
        this.sourceUrl = sourceUrl;
        this.sourceExternalId = sourceExternalId;
        this.imageUrl = imageUrl;
    }
}
