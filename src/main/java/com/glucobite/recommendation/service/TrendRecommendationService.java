package com.glucobite.recommendation.service;

import com.glucobite.glucose.entity.GlucoseMeasurementContext;
import com.glucobite.glucose.entity.GlucoseRecord;
import com.glucobite.glucose.repository.GlucoseRecordRepository;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.exception.HealthProfileNotFoundException;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.meal.entity.MealLog;
import com.glucobite.meal.repository.MealLogRepository;
import com.glucobite.recommendation.dto.TrendMetricsResponse;
import com.glucobite.recommendation.dto.TrendRecommendationItemResponse;
import com.glucobite.recommendation.dto.TrendRecommendationResponse;
import com.glucobite.tracking.service.TrackingDateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrendRecommendationService {

    private static final int MIN_SUFFICIENT_RECORDS = 3;
    private static final BigDecimal HIGH_AVERAGE_SUGAR_PER_MEAL = new BigDecimal("15.00");
    private static final BigDecimal RISING_POST_MEAL_PERCENT = new BigDecimal("10.0");
    private static final String DISCLAIMER =
            "이 권고는 입력한 기록의 생활 패턴 안내이며 의료 진단이나 치료 지시가 아닙니다.";

    private final MealLogRepository mealLogRepository;
    private final GlucoseRecordRepository glucoseRecordRepository;
    private final HealthProfileRepository healthProfileRepository;

    public TrendRecommendationService(
            MealLogRepository mealLogRepository,
            GlucoseRecordRepository glucoseRecordRepository,
            HealthProfileRepository healthProfileRepository
    ) {
        this.mealLogRepository = mealLogRepository;
        this.glucoseRecordRepository = glucoseRecordRepository;
        this.healthProfileRepository = healthProfileRepository;
    }

    @Transactional(readOnly = true)
    public TrendRecommendationResponse get(Long userId, int days) {
        LocalDate to = LocalDate.now(TrackingDateRange.KST);
        LocalDate from = to.minusDays(days - 1L);
        TrackingDateRange range = TrackingDateRange.of(from, to);
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

        TrendMetricsResponse metrics = metrics(meals, glucoseRecords, profile, days, range);
        boolean sufficient = meals.size() >= MIN_SUFFICIENT_RECORDS
                || glucoseRecords.size() >= MIN_SUFFICIENT_RECORDS;
        List<TrendRecommendationItemResponse> recommendations = sufficient
                ? recommendations(metrics, profile)
                : List.of(new TrendRecommendationItemResponse(
                "DATA_INSUFFICIENT",
                "기록을 조금 더 쌓아 주세요",
                "식사 또는 혈당 기록이 3건 이상이면 개인 추세를 비교할 수 있습니다.",
                "현재 식사 %d건, 혈당 %d건".formatted(meals.size(), glucoseRecords.size())
        ));

        return new TrendRecommendationResponse(
                from, to, days, sufficient, metrics, recommendations, DISCLAIMER
        );
    }

    private TrendMetricsResponse metrics(
            List<MealLog> meals,
            List<GlucoseRecord> glucoseRecords,
            HealthProfile profile,
            int days,
            TrackingDateRange range
    ) {
        BigDecimal totalCarb = sumMeals(meals, true);
        BigDecimal totalSugar = sumMeals(meals, false);
        Map<LocalDate, BigDecimal> carbByDay = new LinkedHashMap<>();
        for (MealLog meal : meals) {
            carbByDay.merge(meal.getEatenAt().toLocalDate(), zero(meal.getCarb()), BigDecimal::add);
        }
        long exceededDays = carbByDay.values().stream()
                .filter(value -> value.compareTo(BigDecimal.valueOf(profile.getDailyCarbsTarget())) > 0)
                .count();
        BigDecimal averageGlucose = average(glucoseRecords.stream()
                .map(GlucoseRecord::getValue)
                .toList());
        BigDecimal postMealChange = postMealChange(glucoseRecords, range, days);

        return new TrendMetricsResponse(
                meals.size(),
                glucoseRecords.size(),
                carbByDay.size(),
                Math.toIntExact(exceededDays),
                totalCarb.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP),
                meals.isEmpty() ? null : totalSugar.divide(
                        BigDecimal.valueOf(meals.size()), 2, RoundingMode.HALF_UP
                ),
                averageGlucose,
                postMealChange
        );
    }

    private List<TrendRecommendationItemResponse> recommendations(
            TrendMetricsResponse metrics,
            HealthProfile profile
    ) {
        List<TrendRecommendationItemResponse> items = new ArrayList<>();
        if (metrics.carbTargetExceededDays() > 0) {
            items.add(new TrendRecommendationItemResponse(
                    "CARB_TARGET_EXCEEDED",
                    "탄수화물 양을 나눠 보세요",
                    "목표를 넘긴 날의 한 끼 탄수화물 양을 줄이거나 여러 끼로 분산해 보세요.",
                    "%d일 동안 일일 목표 %dg 초과".formatted(
                            metrics.carbTargetExceededDays(), profile.getDailyCarbsTarget()
                    )
            ));
        }
        if (metrics.averageSugarPerMeal() != null
                && metrics.averageSugarPerMeal().compareTo(HIGH_AVERAGE_SUGAR_PER_MEAL) >= 0) {
            items.add(new TrendRecommendationItemResponse(
                    "SUGAR_PER_MEAL_HIGH",
                    "당류가 적은 대안을 확인해 보세요",
                    "음료와 소스부터 당류가 낮은 선택으로 바꾸면 한 끼 평균을 낮추기 쉽습니다.",
                    "한 끼 평균 당류 %sg".formatted(metrics.averageSugarPerMeal())
            ));
        }
        if (metrics.postMealGlucoseChangePercent() != null
                && metrics.postMealGlucoseChangePercent().compareTo(RISING_POST_MEAL_PERCENT) >= 0) {
            items.add(new TrendRecommendationItemResponse(
                    "POST_MEAL_GLUCOSE_RISING",
                    "최근 식후 기록 변화를 확인해 보세요",
                    "최근 식후 혈당 평균이 이전 기간보다 높았습니다. 같은 식사 조건의 기록을 더 모아 비교해 보세요.",
                    "기간 전반부 대비 후반부 %s%% 변화".formatted(
                            metrics.postMealGlucoseChangePercent()
                    )
            ));
        }
        if (items.isEmpty()) {
            items.add(new TrendRecommendationItemResponse(
                    "MAINTAIN_CURRENT_PATTERN",
                    "현재 기록 습관을 유지해 주세요",
                    "현재 규칙에서 뚜렷한 상승 또는 목표 초과 패턴이 발견되지 않았습니다.",
                    "식사 %d건, 혈당 %d건 분석".formatted(
                            metrics.mealCount(), metrics.glucoseRecordCount()
                    )
            ));
        }
        return List.copyOf(items);
    }

    private BigDecimal postMealChange(
            List<GlucoseRecord> records,
            TrackingDateRange range,
            int days
    ) {
        LocalDateTime split = range.startInclusive().plusDays(days / 2L);
        List<BigDecimal> earlier = records.stream()
                .filter(record -> record.getContext() == GlucoseMeasurementContext.POST_MEAL)
                .filter(record -> record.getMeasuredAt().isBefore(split))
                .map(GlucoseRecord::getValue)
                .toList();
        List<BigDecimal> recent = records.stream()
                .filter(record -> record.getContext() == GlucoseMeasurementContext.POST_MEAL)
                .filter(record -> !record.getMeasuredAt().isBefore(split))
                .map(GlucoseRecord::getValue)
                .toList();
        if (earlier.size() < 2 || recent.size() < 2) {
            return null;
        }
        BigDecimal earlierAverage = average(earlier);
        if (earlierAverage == null || earlierAverage.signum() == 0) {
            return null;
        }
        return average(recent).subtract(earlierAverage)
                .multiply(BigDecimal.valueOf(100))
                .divide(earlierAverage, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumMeals(List<MealLog> meals, boolean carb) {
        return meals.stream()
                .map(meal -> carb ? meal.getCarb() : meal.getSugar())
                .map(this::zero)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
