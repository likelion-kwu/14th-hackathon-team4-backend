package com.glucobite.recipe.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "recipe_steps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recipe_steps_recipe_order",
                        columnNames = {"recipe_id", "step_order"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_step_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    public RecipeStep(
            Recipe recipe,
            Integer stepOrder,
            String description
    ) {
        this.recipe = recipe;
        this.stepOrder = stepOrder;
        this.description = description;
    }
}