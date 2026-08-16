package com.glucobite.health.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import com.glucobite.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(name = "diabetes_status", length = 30)
    private DiabetesStatus diabetesStatus;

    @Column(name = "glucose_device_connected", nullable = false)
    private boolean glucoseDeviceConnected;

    @Column(name = "daily_carbs_target")
    private Integer dailyCarbsTarget;

    @Column(name = "dietary_restriction_note", length = 500)
    private String dietaryRestrictionNote;

    public HealthProfile(
            User user,
            LocalDate birthDate,
            BigDecimal height,
            BigDecimal weight,
            Sex sex,
            DiabetesStatus diabetesStatus,
            boolean glucoseDeviceConnected,
            Integer dailyCarbsTarget,
            String dietaryRestrictionNote
    ) {
        this.user = user;
        this.birthDate = birthDate;
        this.height = height;
        this.weight = weight;
        this.sex = sex;
        this.diabetesStatus = diabetesStatus;
        this.glucoseDeviceConnected = glucoseDeviceConnected;
        this.dailyCarbsTarget = dailyCarbsTarget;
        this.dietaryRestrictionNote = dietaryRestrictionNote;
    }
}