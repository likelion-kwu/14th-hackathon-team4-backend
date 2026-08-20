package com.glucobite.dashboard.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerIntegrationTest {

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
    void summarizesTodayNutritionTargetAndGlucose() throws Exception {
        User user = createProfiledUser("summary-user", 100);
        LocalDate today = LocalDate.now(TrackingDateRange.KST);
        createMeal(user, "아침", "200", "60", "5", today.atTime(8, 0));
        MealLog lunch = createMeal(user, "점심", "400", "60", "10", today.atTime(12, 0));
        createMeal(user, "어제", "999", "999", "999", today.minusDays(1).atTime(12, 0));
        glucoseRecordRepository.saveAll(List.of(
                new GlucoseRecord(user, null, new BigDecimal("100"),
                        GlucoseMeasurementContext.FASTING, today.atTime(7, 0)),
                new GlucoseRecord(user, lunch, new BigDecimal("140"),
                        GlucoseMeasurementContext.POST_MEAL, today.atTime(14, 0))
        ));

        mockMvc.perform(get("/api/dashboard/today")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.mealCount").value(2))
                .andExpect(jsonPath("$.totalCalories").value(600.0))
                .andExpect(jsonPath("$.totalCarb").value(120.0))
                .andExpect(jsonPath("$.totalSugar").value(15.0))
                .andExpect(jsonPath("$.dailyCarbsTarget").value(100))
                .andExpect(jsonPath("$.carbProgressPercent").value(120.0))
                .andExpect(jsonPath("$.glucoseRecordCount").value(2))
                .andExpect(jsonPath("$.averageGlucose").value(120.0))
                .andExpect(jsonPath("$.latestGlucose").value(140.0))
                .andExpect(jsonPath("$.meals[0].title").value("점심"));
    }

    @Test
    void returnsZeroAndNullMetricsWhenTodayHasNoRecords() throws Exception {
        User user = createProfiledUser("empty-summary-user", 180);

        mockMvc.perform(get("/api/dashboard/today")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealCount").value(0))
                .andExpect(jsonPath("$.totalCalories").value(0))
                .andExpect(jsonPath("$.carbProgressPercent").value(0.0))
                .andExpect(jsonPath("$.glucoseRecordCount").value(0))
                .andExpect(jsonPath("$.averageGlucose").doesNotExist())
                .andExpect(jsonPath("$.latestGlucose").doesNotExist())
                .andExpect(jsonPath("$.meals").isEmpty());
    }

    @Test
    void usesInclusiveStartAndExclusiveEndOfKstDay() throws Exception {
        User user = createProfiledUser("boundary-summary-user", 100);
        LocalDate today = LocalDate.now(TrackingDateRange.KST);
        createMeal(user, "자정", "100", "10", "1", today.atStartOfDay());
        createMeal(user, "다음날", "100", "10", "1", today.plusDays(1).atStartOfDay());

        mockMvc.perform(get("/api/dashboard/today")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealCount").value(1))
                .andExpect(jsonPath("$.meals[0].title").value("자정"));
    }

    private User createProfiledUser(String loginId, int dailyCarbsTarget) {
        User user = userRepository.save(new User(loginId, "hash", "요약 사용자"));
        healthProfileRepository.save(new HealthProfile(
                user,
                LocalDate.of(2000, 1, 1),
                new BigDecimal("170"),
                new BigDecimal("65"),
                Sex.FEMALE,
                HealthGoal.CARB_MANAGEMENT,
                null,
                false,
                dailyCarbsTarget,
                VegetarianType.NONE,
                null,
                List.of()
        ));
        return user;
    }

    private MealLog createMeal(
            User user,
            String title,
            String calories,
            String carb,
            String sugar,
            LocalDateTime eatenAt
    ) {
        return mealLogRepository.save(new MealLog(
                user, null, title, null, MealType.LUNCH,
                new BigDecimal(calories), new BigDecimal(carb), new BigDecimal(sugar), eatenAt
        ));
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }
}
