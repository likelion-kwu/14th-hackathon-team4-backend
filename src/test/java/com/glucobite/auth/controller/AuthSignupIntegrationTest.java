package com.glucobite.auth.controller;

import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import com.glucobite.health.repository.AllergenRepository;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import com.glucobite.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @Autowired
    private AllergenRepository allergenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUpBeforeTest() {
        cleanUp();
    }

    @AfterEach
    void cleanUpAfterTest() {
        cleanUp();
    }

    private void cleanUp() {
        refreshTokenRepository.deleteAll();
        healthProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void signsUpWithHealthProfileAndAllergies() throws Exception {
        Long eggId = allergenRepository.findByName("난류").orElseThrow().getId();
        Long milkId = allergenRepository.findByName("우유").orElseThrow().getId();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignupJson("signup-user", eggId + "," + milkId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("HttpOnly")));

        User user = userRepository.findByLoginId("signup-user").orElseThrow();
        HealthProfile profile = healthProfileRepository.findByUserId(user.getId()).orElseThrow();
        Integer allergyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM health_profile_allergies WHERE profile_id = ?",
                Integer.class,
                profile.getId()
        );

        assertThat(user.getNickname()).isEqualTo("테스터");
        assertThat(user.getPassword()).isNotEqualTo("1234");
        assertThat(passwordEncoder.matches("1234", user.getPassword())).isTrue();
        assertThat(profile.getSex()).isEqualTo(Sex.FEMALE);
        assertThat(profile.getHealthGoal()).isEqualTo(HealthGoal.CARB_MANAGEMENT);
        assertThat(profile.getVegetarianType()).isEqualTo(VegetarianType.LACTO_OVO);
        assertThat(profile.isGlucoseDeviceConnected()).isTrue();
        assertThat(allergyCount).isEqualTo(2);
    }

    @Test
    void rejectsDuplicateLoginId() throws Exception {
        userRepository.save(new User("duplicate-user", "encoded-password", "기존 사용자"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignupJson("duplicate-user", "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LOGIN_ID"));

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(healthProfileRepository.count()).isZero();
    }

    @Test
    void rollsBackUserWhenAllergenDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignupJson("rollback-user", "999999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ALLERGEN"));

        assertThat(userRepository.findByLoginId("rollback-user")).isEmpty();
        assertThat(healthProfileRepository.count()).isZero();
    }

    @Test
    void rejectsInvalidProfileAndUnspecifiedSex() throws Exception {
        String invalidJson = validSignupJson("invalid-user", "")
                .replace("\"nickname\":\"테스터\"", "\"nickname\":\"\"")
                .replace("\"birthDate\":\"2000-01-01\"", "\"birthDate\":\"2999-01-01\"")
                .replace("\"sex\":\"FEMALE\"", "\"sex\":\"UNSPECIFIED\"");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        assertThat(userRepository.findByLoginId("invalid-user")).isEmpty();
    }

    @Test
    void rejectsMissingProfileValuesAtValidationBoundary() throws Exception {
        String invalidJson = validSignupJson("boundary-user", "")
                .replace("\"nickname\":\"테스터\"", "\"nickname\":\"\"")
                .replace("\"height\":165.50", "\"height\":0")
                .replace("\"dailyCarbsTarget\":180", "\"dailyCarbsTarget\":0");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.nickname").exists())
                .andExpect(jsonPath("$.fieldErrors['profile.height']").exists())
                .andExpect(jsonPath("$.fieldErrors['profile.dailyCarbsTarget']").exists());

        assertThat(userRepository.findByLoginId("boundary-user")).isEmpty();
    }

    @Test
    void acceptsCurrentBirthDate() throws Exception {
        String currentBirthDateJson = validSignupJson("current-date-user", "")
                .replace("2000-01-01", LocalDate.now().toString());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(currentBirthDateJson))
                .andExpect(status().isCreated());

        assertThat(userRepository.findByLoginId("current-date-user")).isPresent();
    }

    @Test
    void rejectsFutureBirthDate() throws Exception {
        String futureBirthDateJson = validSignupJson("future-date-user", "")
                .replace("2000-01-01", LocalDate.now().plusDays(1).toString());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(futureBirthDateJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors['profile.birthDate']").exists());

        assertThat(userRepository.findByLoginId("future-date-user")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "12345", "12a4", "１２３４"})
    void rejectsPasswordsThatAreNotExactlyFourAsciiDigits(String password) throws Exception {
        String invalidPasswordJson = validSignupJson("invalid-password-user", "")
                .replace("\"password\":\"1234\"", "\"password\":\"%s\"".formatted(password));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPasswordJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        assertThat(userRepository.findByLoginId("invalid-password-user")).isEmpty();
    }

    @Test
    void rejectsNullAllergenIdAtValidationBoundary() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSignupJson("null-allergen-user", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors['profile.allergenIds[]']").exists());

        assertThat(userRepository.findByLoginId("null-allergen-user")).isEmpty();
    }

    private String validSignupJson(String loginId, String allergenIds) {
        return """
                {
                  "loginId":"%s",
                  "password":"1234",
                  "nickname":"테스터",
                  "profile":{
                    "birthDate":"2000-01-01",
                    "height":165.50,
                    "weight":55.20,
                    "sex":"FEMALE",
                    "healthGoal":"CARB_MANAGEMENT",
                    "dailyCarbsTarget":180,
                    "glucoseDeviceConnected":true,
                    "vegetarianType":"LACTO_OVO",
                    "allergenIds":[%s],
                    "dietaryRestrictionNote":"갑각류 제외"
                  }
                }
                """.formatted(loginId, allergenIds);
    }
}
