package com.glucobite.health.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import com.glucobite.health.repository.AllergenRepository;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class HealthProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @Autowired
    private AllergenRepository allergenRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnsOnlyAuthenticatedUsersProfileWithSortedAllergens() throws Exception {
        User owner = saveUser("health-profile-owner");
        User anotherUser = saveUser("health-profile-other");
        Allergen egg = allergenRepository.findByName("난류").orElseThrow();
        Allergen milk = allergenRepository.findByName("우유").orElseThrow();
        saveProfile(owner, List.of(milk, egg));
        saveProfile(anotherUser, List.of());

        mockMvc.perform(get("/api/health/profile")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.height").value(165.50))
                .andExpect(jsonPath("$.weight").value(55.20))
                .andExpect(jsonPath("$.sex").value("FEMALE"))
                .andExpect(jsonPath("$.healthGoal").value("CARB_MANAGEMENT"))
                .andExpect(jsonPath("$.dailyCarbsTarget").value(180))
                .andExpect(jsonPath("$.glucoseDeviceConnected").value(true))
                .andExpect(jsonPath("$.vegetarianType").value("LACTO_OVO"))
                .andExpect(jsonPath("$.allergens", hasSize(2)))
                .andExpect(jsonPath("$.allergens[0].name").value("난류"))
                .andExpect(jsonPath("$.allergens[1].name").value("우유"))
                .andExpect(jsonPath("$.dietaryRestrictionNote").value("갑각류 제외"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.diabetesStatus").doesNotExist());
    }

    @Test
    void replacesEntireProfileAndClearsAllergensAndBlankNote() throws Exception {
        User owner = saveUser("health-profile-update-owner");
        Allergen milk = allergenRepository.findByName("우유").orElseThrow();
        HealthProfile profile = saveProfile(owner, List.of(milk));
        entityManager.clear();

        mockMvc.perform(put("/api/health/profile")
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson("[]", "   ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.height").value(166.00))
                .andExpect(jsonPath("$.weight").value(54.80))
                .andExpect(jsonPath("$.healthGoal").value("WEIGHT_MANAGEMENT"))
                .andExpect(jsonPath("$.dailyCarbsTarget").value(170))
                .andExpect(jsonPath("$.glucoseDeviceConnected").value(false))
                .andExpect(jsonPath("$.vegetarianType").value("PESCATARIAN"))
                .andExpect(jsonPath("$.allergens", hasSize(0)))
                .andExpect(jsonPath("$.dietaryRestrictionNote").value(nullValue()));

        entityManager.clear();
        HealthProfile updated = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(updated.getAllergens()).isEmpty();
        assertThat(updated.getDietaryRestrictionNote()).isNull();
    }

    @Test
    void rollsBackEntireUpdateWhenAllergenDoesNotExist() throws Exception {
        User owner = saveUser("health-profile-rollback-owner");
        Allergen milk = allergenRepository.findByName("우유").orElseThrow();
        HealthProfile profile = saveProfile(owner, List.of(milk));

        mockMvc.perform(put("/api/health/profile")
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson("[999999]", "변경 시도")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ALLERGEN"));

        entityManager.clear();
        HealthProfile unchanged = healthProfileRepository.findById(profile.getId()).orElseThrow();
        assertThat(unchanged.getHeight()).isEqualByComparingTo("165.50");
        assertThat(unchanged.getDietaryRestrictionNote()).isEqualTo("갑각류 제외");
        assertThat(unchanged.getAllergens()).extracting(Allergen::getName)
                .containsExactly("우유");
    }

    @Test
    void returnsNotFoundWhenAuthenticatedUserHasNoProfile() throws Exception {
        User userWithoutProfile = saveUser("health-profile-missing-owner");

        mockMvc.perform(get("/api/health/profile")
                        .header("Authorization", bearerToken(userWithoutProfile)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HEALTH_PROFILE_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidProfileValuesBeforeUpdate() throws Exception {
        User owner = saveUser("health-profile-invalid-owner");
        saveProfile(owner, List.of());
        String invalidRequest = updateRequestJson("[null]", "메모")
                .replace("2000-01-01", LocalDate.now().plusDays(1).toString())
                .replace("166.00", "0");

        mockMvc.perform(put("/api/health/profile")
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.birthDate").exists())
                .andExpect(jsonPath("$.fieldErrors.height").exists())
                .andExpect(jsonPath("$.fieldErrors['allergenIds[]']").exists());
    }

    @Test
    void rejectsUnspecifiedSex() throws Exception {
        User owner = saveUser("health-profile-sex-owner");
        saveProfile(owner, List.of());
        String invalidRequest = updateRequestJson("[]", "메모")
                .replace("FEMALE", "UNSPECIFIED");

        mockMvc.perform(put("/api/health/profile")
                        .header("Authorization", bearerToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void requiresAuthenticationForReadAndUpdate() throws Exception {
        mockMvc.perform(get("/api/health/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/health/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequestJson("[]", "메모")))
                .andExpect(status().isUnauthorized());
    }

    private User saveUser(String loginId) {
        return userRepository.saveAndFlush(
                new User(loginId, "encoded-password", "건강 프로필 사용자")
        );
    }

    private HealthProfile saveProfile(User user, List<Allergen> allergens) {
        return healthProfileRepository.saveAndFlush(new HealthProfile(
                user,
                LocalDate.of(2000, 1, 1),
                new BigDecimal("165.50"),
                new BigDecimal("55.20"),
                Sex.FEMALE,
                HealthGoal.CARB_MANAGEMENT,
                null,
                true,
                180,
                VegetarianType.LACTO_OVO,
                "갑각류 제외",
                allergens
        ));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }

    private String updateRequestJson(String allergenIds, String note) {
        return """
                {
                  "birthDate":"2000-01-01",
                  "height":166.00,
                  "weight":54.80,
                  "sex":"FEMALE",
                  "healthGoal":"WEIGHT_MANAGEMENT",
                  "dailyCarbsTarget":170,
                  "glucoseDeviceConnected":false,
                  "vegetarianType":"PESCATARIAN",
                  "allergenIds":%s,
                  "dietaryRestrictionNote":"%s"
                }
                """.formatted(allergenIds, note);
    }
}
