package com.glucobite.ingredient.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "ingredient_substitutes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ingredient_substitute",
                        columnNames = {"ingredient_id", "substitute_ingredient_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientSubstitute extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredient_substitute_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitute_ingredient_id", nullable = false)
    private Ingredient substitute;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal ratio;

    @Size(max = 500)
    @Column(length = 500)
    private String reason;

    public IngredientSubstitute(
            Ingredient ingredient,
            Ingredient substitute,
            BigDecimal ratio,
            String reason
    ) {
        this.ingredient = ingredient;
        this.substitute = substitute;
        this.ratio = ratio;
        this.reason = reason;
    }
}