package com.glucobite.meal.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meal_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @PositiveOrZero
    @Column(precision = 6, scale = 2)
    private BigDecimal glucose;

    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal carb;

    @PositiveOrZero
    @Column(precision = 10, scale = 2)
    private BigDecimal sugar;

    @Column(name = "eaten_at", nullable = false)
    private LocalDateTime eatenAt;

    public MealLog(
            User user,
            Recipe recipe,
            String imageUrl,
            MealType mealType,
            BigDecimal glucose,
            BigDecimal carb,
            BigDecimal sugar,
            LocalDateTime eatenAt
    ) {
        this.user = user;
        this.recipe = recipe;
        this.imageUrl = imageUrl;
        this.mealType = mealType;
        this.glucose = glucose;
        this.carb = carb;
        this.sugar = sugar;
        this.eatenAt = eatenAt;
    }
}
