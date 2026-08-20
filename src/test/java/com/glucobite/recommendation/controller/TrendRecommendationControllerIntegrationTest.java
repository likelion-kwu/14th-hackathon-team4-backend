package com.glucobite.recommendation.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.glucose.entity.GlucoseMeasurementContext;
import com.glucobite.glucose.entity.GlucoseRecord;
import com.glucobite.glucose.repository.GlucoseRecordRepository;
import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.meal.entity.MealLog;
import com.glucobite.meal.entity.MealType;
import com.glucobite.meal.repository.MealLogRepository;
import com.glucobite.tracking.service.TrackingDateRange;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TrendRecommendationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HealthProfileRepository healthProfileRepository;
    @Autowired private MealLogRepository mealLogRepository;
    @Autowired private GlucoseRecordRepository glucoseRecordRepository;
    @Autowired private JwtTokenService jwtTokenService;

    @AfterEach
    void cleanUp() {
        glucoseRecordRepository.deleteAll();
        mealLogRepository.deleteAll();
        healthProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void returnsDataInsufficientRecommendationWithoutInventingTrend() throws Exception {
        User user = createProfiledUser("insufficient-user", 120);
        createMeal(user, "한 끼", "30", "5",
                LocalDateTime.now(TrackingDateRange.KST).minusHours(1));

        mockMvc.perform(get("/api/recommendations/trends")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.dataSufficient").value(false))
                .andExpect(jsonPath("$.recommendations[0].code").value("DATA_INSUFFICIENT"))
                .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void explainsCarbSugarAndRisingPostMealPatterns() throws Exception {
        User user = createProfiledUser("trend-user", 100);
        User other = createProfiledUser("other-trend-user", 50);
        LocalDate today = LocalDate.now(TrackingDateRange.KST);
        createMeal(user, "식사1", "120", "20", today.minusDays(3).atTime(12, 0));
        createMeal(user, "식사2", "40", "20", today.minusDays(2).atTime(12, 0));
        createMeal(user, "식사3", "40", "20", today.minusDays(1).atTime(12, 0));
        createMeal(user, "식사4", "40", "20", today.atTime(12, 0));
        createMeal(other, "다른 사용자", "999", "999", today.atTime(12, 0));
        glucoseRecordRepository.saveAll(List.of(
                glucose(user, "100", today.minusDays(6).atTime(14, 0)),
                glucose(user, "110", today.minusDays(5).atTime(14, 0)),
                glucose(user, "140", today.minusDays(1).atTime(14, 0)),
                glucose(user, "150", today.atTime(14, 0)),
                glucose(other, "600", today.atTime(15, 0))
        ));

        mockMvc.perform(get("/api/recommendations/trends")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataSufficient").value(true))
                .andExpect(jsonPath("$.metrics.mealCount").value(4))
                .andExpect(jsonPath("$.metrics.glucoseRecordCount").value(4))
                .andExpect(jsonPath("$.metrics.carbTargetExceededDays").value(1))
                .andExpect(jsonPath("$.metrics.averageSugarPerMeal").value(20.0))
                .andExpect(jsonPath("$.metrics.postMealGlucoseChangePercent").value(38.1))
                .andExpect(jsonPath("$.recommendations[*].code", containsInAnyOrder(
                        "CARB_TARGET_EXCEEDED",
                        "SUGAR_PER_MEAL_HIGH",
                        "POST_MEAL_GLUCOSE_RISING"
                )));
    }

    @Test
    void returnsMaintainRecommendationWhenRulesDoNotTrigger() throws Exception {
        User user = createProfiledUser("maintain-user", 200);
        LocalDate today = LocalDate.now(TrackingDateRange.KST);
        createMeal(user, "식사1", "30", "2", today.minusDays(2).atTime(12, 0));
        createMeal(user, "식사2", "30", "2", today.minusDays(1).atTime(12, 0));
        createMeal(user, "식사3", "30", "2", today.atTime(12, 0));

        mockMvc.perform(get("/api/recommendations/trends")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[0].code")
                        .value("MAINTAIN_CURRENT_PATTERN"));
    }

    @Test
    void rejectsDaysOutsideSupportedRange() throws Exception {
        User user = createProfiledUser("days-user", 150);

        mockMvc.perform(get("/api/recommendations/trends")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .param("days", "6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/recommendations/trends")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .param("days", "31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private User createProfiledUser(String loginId, int target) {
        User user = userRepository.save(new User(loginId, "hash", "추천 사용자"));
        healthProfileRepository.save(new HealthProfile(
                user,
                LocalDate.of(2000, 1, 1),
                new BigDecimal("170"),
                new BigDecimal("65"),
                Sex.MALE,
                HealthGoal.CARB_MANAGEMENT,
                null,
                false,
                target,
                VegetarianType.NONE,
                null,
                List.of()
        ));
        return user;
    }

    private MealLog createMeal(
            User user,
            String title,
            String carb,
            String sugar,
            LocalDateTime eatenAt
    ) {
        return mealLogRepository.save(new MealLog(
                user, null, title, null, MealType.LUNCH,
                new BigDecimal("300"), new BigDecimal(carb), new BigDecimal(sugar), eatenAt
        ));
    }

    private GlucoseRecord glucose(User user, String value, LocalDateTime measuredAt) {
        return new GlucoseRecord(
                user, null, new BigDecimal(value),
                GlucoseMeasurementContext.POST_MEAL, measuredAt
        );
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }
}
