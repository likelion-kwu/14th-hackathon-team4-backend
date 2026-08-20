package com.glucobite.dashboard.service;

import com.glucobite.dashboard.dto.TodaySummaryResponse;
import com.glucobite.glucose.entity.GlucoseRecord;
import com.glucobite.glucose.repository.GlucoseRecordRepository;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.meal.dto.MealLogResponse;
import com.glucobite.meal.entity.MealLog;
import com.glucobite.meal.repository.MealLogRepository;
import com.glucobite.tracking.service.TrackingDateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class TodaySummaryService {

    private final MealLogRepository mealLogRepository;
    private final GlucoseRecordRepository glucoseRecordRepository;
    private final HealthProfileRepository healthProfileRepository;

    public TodaySummaryService(
            MealLogRepository mealLogRepository,
            GlucoseRecordRepository glucoseRecordRepository,
            HealthProfileRepository healthProfileRepository
    ) {
        this.mealLogRepository = mealLogRepository;
        this.glucoseRecordRepository = glucoseRecordRepository;
        this.healthProfileRepository = healthProfileRepository;
    }

    @Transactional(readOnly = true)
    public TodaySummaryResponse get(Long userId) {
        LocalDate today = LocalDate.now(TrackingDateRange.KST);
        TrackingDateRange range = TrackingDateRange.of(today, today);
        HealthProfile profile = healthProfileRepository.findByUserId(userId)
                .orElseThrow(HealthProfileNotFoundException::new);
        List<MealLog> meals = mealLogRepository
                .findByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtDescIdDesc(
                        userId, range.startInclusive(), range.endExclusive()
                );
        List<GlucoseRecord> glucoseRecords = glucoseRecordRepository
                .findByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(
                        userId, range.startInclusive(), range.endExclusive()
                );

        BigDecimal totalCalories = sum(meals, NutritionField.CALORIES);
        BigDecimal totalCarb = sum(meals, NutritionField.CARB);
        BigDecimal totalSugar = sum(meals, NutritionField.SUGAR);
        BigDecimal averageGlucose = glucoseRecords.isEmpty()
                ? null
                : glucoseRecords.stream()
                .map(GlucoseRecord::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(glucoseRecords.size()), 2, RoundingMode.HALF_UP);
        GlucoseRecord latest = glucoseRecords.isEmpty() ? null : glucoseRecords.getFirst();

        return new TodaySummaryResponse(
                today,
                meals.size(),
                totalCalories,
                totalCarb,
                totalSugar,
                profile.getDailyCarbsTarget(),
                progress(totalCarb, profile.getDailyCarbsTarget()),
                glucoseRecords.size(),
                averageGlucose,
                latest == null ? null : latest.getValue(),
                latest == null ? null : latest.getMeasuredAt(),
                meals.stream().map(MealLogResponse::from).toList()
        );
    }

    private BigDecimal sum(List<MealLog> meals, NutritionField field) {
        return meals.stream()
                .map(field::value)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal progress(BigDecimal consumed, Integer target) {
        if (target == null || target <= 0) {
            return null;
        }
        return consumed.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(target), 1, RoundingMode.HALF_UP);
    }

    private enum NutritionField {
        CALORIES {
            @Override BigDecimal value(MealLog meal) { return meal.getCalories(); }
        },
        CARB {
            @Override BigDecimal value(MealLog meal) { return meal.getCarb(); }
        },
        SUGAR {
            @Override BigDecimal value(MealLog meal) { return meal.getSugar(); }
        };

        abstract BigDecimal value(MealLog meal);
    }
}
