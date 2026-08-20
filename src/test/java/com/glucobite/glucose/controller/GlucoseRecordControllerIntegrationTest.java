package com.glucobite.glucose.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.glucose.repository.GlucoseRecordRepository;
import com.glucobite.meal.entity.MealLog;
import com.glucobite.meal.entity.MealType;
import com.glucobite.meal.repository.MealLogRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import com.glucobite.tracking.service.TrackingDateRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlucoseRecordControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private MealLogRepository mealLogRepository;
    @Autowired private GlucoseRecordRepository glucoseRecordRepository;
    @Autowired private JwtTokenService jwtTokenService;

    @AfterEach
    void cleanUp() {
        glucoseRecordRepository.deleteAll();
        mealLogRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void recordsLinkedGlucoseAndReturnsItByDate() throws Exception {
        User user = createUser("glucose-user");
        MealLog meal = createMeal(user, "점심");
        String measuredAt = LocalDateTime.now(TrackingDateRange.KST)
                .minusMinutes(30).withNano(0).toString();

        mockMvc.perform(post("/api/glucose-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mealLogId":%d,
                                  "value":128.5,
                                  "context":"POST_MEAL",
                                  "measuredAt":"%s"
                                }
                                """.formatted(meal.getId(), measuredAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mealLogId").value(meal.getId()))
                .andExpect(jsonPath("$.value").value(128.5))
                .andExpect(jsonPath("$.context").value("POST_MEAL"));

        String today = LocalDate.now(TrackingDateRange.KST).toString();
        mockMvc.perform(get("/api/glucose-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].value").value(128.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"20.00", "600.00"})
    void acceptsGlucoseBoundaryValues(String value) throws Exception {
        User user = createUser("boundary-" + value.replace('.', '-'));

        mockMvc.perform(post("/api/glucose-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(value, "FASTING")))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"19.99", "600.01", "-1"})
    void rejectsOutOfRangeGlucose(String value) throws Exception {
        User user = createUser("invalid-" + value.replace('.', '-').replace("-", "n"));

        mockMvc.perform(post("/api/glucose-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(value, "OTHER")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsLinkToOtherUsersMeal() throws Exception {
        User owner = createUser("meal-owner");
        User other = createUser("glucose-other");
        MealLog meal = createMeal(owner, "소유자 식사");
        String measuredAt = LocalDateTime.now(TrackingDateRange.KST)
                .minusMinutes(1).withNano(0).toString();

        mockMvc.perform(post("/api/glucose-records")
                        .header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mealLogId":%d,"value":100,"context":"POST_MEAL","measuredAt":"%s"}
                                """.formatted(meal.getId(), measuredAt)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEAL_LOG_NOT_FOUND"));
    }

    private String requestJson(String value, String context) {
        return """
                {"value":%s,"context":"%s","measuredAt":"%s"}
                """.formatted(
                value,
                context,
                LocalDateTime.now(TrackingDateRange.KST).minusSeconds(1).withNano(0)
        );
    }

    private User createUser(String loginId) {
        return userRepository.save(new User(loginId, "hash", "혈당 사용자"));
    }

    private MealLog createMeal(User user, String title) {
        return mealLogRepository.save(new MealLog(
                user,
                null,
                title,
                null,
                MealType.LUNCH,
                new BigDecimal("300"),
                new BigDecimal("40"),
                new BigDecimal("5"),
                LocalDateTime.now(TrackingDateRange.KST).minusHours(1)
        ));
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }
}
