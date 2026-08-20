package com.glucobite.glucose.entity;

import com.glucobite.common.entity.BaseTimeEntity;
import com.glucobite.meal.entity.MealLog;
import com.glucobite.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "glucose_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GlucoseRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "glucose_record_id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_log_id")
    private MealLog mealLog;

    @NotNull
    @DecimalMin("20.00")
    @DecimalMax("600.00")
    @Column(name = "glucose_value", nullable = false, precision = 6, scale = 2)
    private BigDecimal value;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_context", nullable = false, length = 30)
    private GlucoseMeasurementContext context;

    @NotNull
    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    public GlucoseRecord(
            User user,
            MealLog mealLog,
            BigDecimal value,
            GlucoseMeasurementContext context,
            LocalDateTime measuredAt
    ) {
        this.user = user;
        this.mealLog = mealLog;
        this.value = value;
        this.context = context;
        this.measuredAt = measuredAt;
    }
}
