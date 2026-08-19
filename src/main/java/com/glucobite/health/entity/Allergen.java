package com.glucobite.health.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "allergens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_allergens_name", columnNames = "name")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Allergen extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allergen_id")
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    public Allergen(String name) {
        this.name = name;
    }
}
