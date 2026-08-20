package com.glucobite.health.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import com.glucobite.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "health_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_health_profiles_user_id",
                        columnNames = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @PastOrPresent
    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal height;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sex sex;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "health_goal", nullable = false, length = 40)
    private HealthGoal healthGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "diabetes_status", length = 30)
    private DiabetesStatus diabetesStatus;

    @Column(name = "glucose_device_connected", nullable = false)
    private boolean glucoseDeviceConnected;

    @NotNull
    @Positive
    @Column(name = "daily_carbs_target", nullable = false)
    private Integer dailyCarbsTarget;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "vegetarian_type", nullable = false, length = 30)
    private VegetarianType vegetarianType;

    @Size(max = 500)
    @Column(name = "dietary_restriction_note", length = 500)
    private String dietaryRestrictionNote;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "health_profile_allergies",
            joinColumns = @JoinColumn(name = "profile_id"),
            inverseJoinColumns = @JoinColumn(name = "allergen_id"),
            uniqueConstraints = {
                    @UniqueConstraint(
                            name = "uk_health_profile_allergies",
                            columnNames = {"profile_id", "allergen_id"}
                    )
            }
    )
    private Set<Allergen> allergens = new LinkedHashSet<>();

    public HealthProfile(
            User user,
            LocalDate birthDate,
            BigDecimal height,
            BigDecimal weight,
            Sex sex,
            HealthGoal healthGoal,
            DiabetesStatus diabetesStatus,
            boolean glucoseDeviceConnected,
            Integer dailyCarbsTarget,
            VegetarianType vegetarianType,
            String dietaryRestrictionNote,
            Collection<Allergen> allergens
    ) {
        this.user = user;
        this.birthDate = birthDate;
        this.height = height;
        this.weight = weight;
        this.sex = sex;
        this.healthGoal = healthGoal;
        this.diabetesStatus = diabetesStatus;
        this.glucoseDeviceConnected = glucoseDeviceConnected;
        this.dailyCarbsTarget = dailyCarbsTarget;
        this.vegetarianType = vegetarianType;
        this.dietaryRestrictionNote = dietaryRestrictionNote;
        this.allergens.addAll(allergens);
    }

    public void update(
            LocalDate birthDate,
            BigDecimal height,
            BigDecimal weight,
            Sex sex,
            HealthGoal healthGoal,
            boolean glucoseDeviceConnected,
            Integer dailyCarbsTarget,
            VegetarianType vegetarianType,
            String dietaryRestrictionNote,
            Collection<Allergen> allergens
    ) {
        this.birthDate = birthDate;
        this.height = height;
        this.weight = weight;
        this.sex = sex;
        this.healthGoal = healthGoal;
        this.glucoseDeviceConnected = glucoseDeviceConnected;
        this.dailyCarbsTarget = dailyCarbsTarget;
        this.vegetarianType = vegetarianType;
        this.dietaryRestrictionNote = dietaryRestrictionNote;
        this.allergens.clear();
        this.allergens.addAll(allergens);
    }
}
