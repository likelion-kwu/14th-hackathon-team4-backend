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
    }

    public void complete() {
        this.completed = true;
    }
}
