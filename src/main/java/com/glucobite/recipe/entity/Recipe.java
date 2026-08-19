package com.glucobite.recipe.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import com.glucobite.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    public Recipe(
            User user,
            String title,
            String description,
            Integer cookingTime
    ) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.cookingTime = cookingTime;
        this.completed = false;
    }

    public void complete() {
        this.completed = true;
    }
}
