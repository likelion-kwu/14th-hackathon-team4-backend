package com.glucobite.recipe.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class RecipeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnsOnlyOwnersRecipesInStableDefaultOrder() throws Exception {
        User owner = saveUser("recipe-list-owner");
        User anotherUser = saveUser("recipe-list-other");
        Recipe oldest = saveRecipe(owner, "오래된 레시피", false, RecipeImportType.TEXT, "120.00");
        Recipe sameTimeLowerId = saveRecipe(owner, "동시 생성 낮은 ID", false, RecipeImportType.URL, "430.50");
        Recipe sameTimeHigherId = saveRecipe(owner, "동시 생성 높은 ID", true, RecipeImportType.IMAGE, "250.00");
        Recipe otherRecipe = saveRecipe(anotherUser, "다른 사용자 레시피", true, null, null);

        updateCreatedAt(oldest, LocalDateTime.of(2026, 8, 18, 10, 0));
        updateCreatedAt(sameTimeLowerId, LocalDateTime.of(2026, 8, 19, 10, 0));
        updateCreatedAt(sameTimeHigherId, LocalDateTime.of(2026, 8, 19, 10, 0));
        updateCreatedAt(otherRecipe, LocalDateTime.of(2026, 8, 20, 10, 0));
        entityManager.clear();

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[0].recipeId").value(sameTimeHigherId.getId()))
                .andExpect(jsonPath("$.content[1].recipeId").value(sameTimeLowerId.getId()))
                .andExpect(jsonPath("$.content[2].recipeId").value(oldest.getId()))
                .andExpect(jsonPath("$.content[0].title").value("동시 생성 높은 ID"))
                .andExpect(jsonPath("$.content[0].cookingTime").value(20))
                .andExpect(jsonPath("$.content[0].totalCalories").value(250.00))
                .andExpect(jsonPath("$.content[0].importType").value("IMAGE"))
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-08-19T10:00:00"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void filtersCompletedAndIncompleteRecipes() throws Exception {
        User owner = saveUser("recipe-filter-owner");
        saveRecipe(owner, "완료 레시피", true, null, null);
        saveRecipe(owner, "미완료 레시피", false, null, null);

        mockMvc.perform(get("/api/recipes")
                        .param("completed", "true")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("완료 레시피"));

        mockMvc.perform(get("/api/recipes")
                        .param("completed", "false")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("미완료 레시피"));
    }

    @Test
    void returnsFirstLastAndEmptyPagesWithMetadata() throws Exception {
        User owner = saveUser("recipe-page-owner");
        saveRecipe(owner, "레시피 1", false, null, null);
        saveRecipe(owner, "레시피 2", false, null, null);
        saveRecipe(owner, "레시피 3", false, null, null);

        mockMvc.perform(get("/api/recipes")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/recipes")
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(get("/api/recipes")
                        .param("page", "2")
                        .param("size", "2")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(2));
    }

    @Test
    void returnsNullForUnanalyzedMetadata() throws Exception {
        User owner = saveUser("recipe-null-metadata-owner");
        saveRecipe(owner, "분석 전 레시피", false, null, null);

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", bearerToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].totalCalories").value(nullValue()))
                .andExpect(jsonPath("$.content[0].importType").value(nullValue()));
    }

    @Test
    void rejectsInvalidPageAndSize() throws Exception {
        User owner = saveUser("recipe-invalid-page-owner");
        String token = bearerToken(owner);

        mockMvc.perform(get("/api/recipes")
                        .param("page", "-1")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.page").exists());

        mockMvc.perform(get("/api/recipes")
                        .param("size", "0")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.size").exists());

        mockMvc.perform(get("/api/recipes")
                        .param("size", "101")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.size").exists());

        mockMvc.perform(get("/api/recipes")
                        .param("completed", "done")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.completed")
                        .value("요청 값 형식이 올바르지 않습니다."));
    }

    @Test
    void rejectsMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/recipes"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTamperedAndExpiredTokens() throws Exception {
        String token = jwtTokenService.issue(42L).accessToken();
        int signatureStart = token.lastIndexOf('.') + 1;
        char signatureCharacter = token.charAt(signatureStart);
        char replacement = signatureCharacter == 'a' ? 'b' : 'a';
        String tamperedToken = token.substring(0, signatureStart)
                + replacement
                + token.substring(signatureStart + 1);

        Instant now = Instant.now();
        JwtClaimsSet expiredClaims = JwtClaimsSet.builder()
                .issuer("https://api.glucobite.app")
                .issuedAt(now.minusSeconds(120))
                .expiresAt(now.minusSeconds(60))
                .subject("42")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String expiredToken = jwtEncoder.encode(JwtEncoderParameters.from(header, expiredClaims))
                .getTokenValue();

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsEmptyPageForValidTokenWhoseUserDoesNotExist() throws Exception {
        String token = "Bearer " + jwtTokenService.issue(Long.MAX_VALUE).accessToken();

        mockMvc.perform(get("/api/recipes")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private User saveUser(String loginId) {
        return userRepository.saveAndFlush(
                new User(loginId, "encoded-password", "레시피 사용자")
        );
    }

    private Recipe saveRecipe(
            User user,
            String title,
            boolean completed,
            RecipeImportType importType,
            String totalCalories
    ) {
        Recipe recipe = new Recipe(
                user,
                title,
                null,
                20,
                importType,
                totalCalories == null ? null : new BigDecimal(totalCalories)
        );
        if (completed) {
            recipe.complete();
        }
        return recipeRepository.saveAndFlush(recipe);
    }

    private void updateCreatedAt(Recipe recipe, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE recipes SET created_at = ? WHERE recipe_id = ?",
                createdAt,
                recipe.getId()
        );
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }
}
