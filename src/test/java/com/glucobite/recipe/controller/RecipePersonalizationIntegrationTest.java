package com.glucobite.recipe.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.health.entity.Allergen;
import com.glucobite.health.entity.HealthGoal;
import com.glucobite.health.entity.HealthProfile;
import com.glucobite.health.entity.Sex;
import com.glucobite.health.entity.VegetarianType;
import com.glucobite.health.repository.AllergenRepository;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.entity.IngredientSubstitute;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientRepository;
import com.glucobite.ingredient.repository.IngredientSubstituteRepository;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.entity.RecipeStep;
import com.glucobite.recipe.entity.RecipeType;
import com.glucobite.recipe.personalization.GeneratedPersonalization;
import com.glucobite.recipe.personalization.RecipePersonalizationGenerator;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class RecipePersonalizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HealthProfileRepository healthProfileRepository;
    @Autowired private AllergenRepository allergenRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IngredientNutritionRepository ingredientNutritionRepository;
    @Autowired private IngredientSubstituteRepository ingredientSubstituteRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private RecipeIngredientRepository recipeIngredientRepository;
    @Autowired private RecipeStepRepository recipeStepRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private RecipePersonalizationGenerator personalizationGenerator;

    private User owner;
    private User anotherUser;
    private Recipe recipe;
    private Ingredient pork;
    private Ingredient milk;
    private Ingredient chicken;
    private Ingredient rice;
    private Ingredient cauliflower;

    @BeforeEach
    void setUp() {
        cleanUp();
        pork = saveIngredient("돼지고기", "250.00", "0.00", "20.00");
        milk = saveIngredient("우유", "60.00", "0.00", "3.00");
        chicken = saveIngredient("닭가슴살", "165.00", "0.00", "31.00");
        rice = saveIngredient("쌀", "100.00", "25.00", "2.00");
        cauliflower = saveIngredient("콜리플라워", "25.00", "5.00", "2.00");

        ingredientSubstituteRepository.save(new IngredientSubstitute(
                pork, milk, new BigDecimal("1.0000"), "첫 번째지만 알레르기 후보"
        ));
        ingredientSubstituteRepository.save(new IngredientSubstitute(
                pork, chicken, new BigDecimal("0.8000"), "안전한 저지방 후보"
        ));
        ingredientSubstituteRepository.save(new IngredientSubstitute(
                rice, cauliflower, new BigDecimal("1.0000"), "탄수화물 감소 후보"
        ));

        owner = userRepository.save(new User("personal-owner", "encoded-password", "소유자"));
        anotherUser = userRepository.save(new User("personal-other", "encoded-password", "다른 사용자"));
        saveProfile(owner, VegetarianType.NONE, "돼지고기", "우유");

        recipe = recipeRepository.save(new Recipe(owner, "돼지고기 볶음", null, 20));
        recipeIngredientRepository.save(new RecipeIngredient(recipe, pork, BigDecimal.ONE));
        recipeIngredientRepository.save(new RecipeIngredient(recipe, rice, BigDecimal.ONE));
        recipeStepRepository.save(new RecipeStep(recipe, 2, "볶는다."));
        recipeStepRepository.save(new RecipeStep(recipe, 1, "재료를 손질한다."));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void generatesAndPersistsGptPersonalizationCandidate() throws Exception {
        when(personalizationGenerator.generate(any())).thenReturn(generatedCandidate(
                "고단백질 위주 수정안",
                "닭가슴살 콜리플라워 볶음",
                chicken.getId(),
                cauliflower.getId()
        ));

        MvcResult result = mockMvc.perform(post("/api/recipes/{id}/personalized", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.originalRecipeId").value(recipe.getId()))
                .andExpect(jsonPath("$.label").value("고단백질 위주 수정안"))
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(chicken.getId()))
                .andExpect(jsonPath("$.ingredients[0].changed").value(true))
                .andExpect(jsonPath("$.originalNutrition.calories").value(350.0000))
                .andExpect(jsonPath("$.personalizedNutrition.calories").value(190.0000))
                .andReturn();

        Long candidateId = objectMapper.readTree(result.getResponse().getContentAsByteArray())
                .get("candidateRecipeId").asLong();
        Recipe candidate = recipeRepository.findById(candidateId).orElseThrow();
        assertThat(candidate.getRecipeType()).isEqualTo(RecipeType.PERSONALIZATION_CANDIDATE);
        assertThat(candidate.isCompleted()).isFalse();
        assertThat(candidate.getSourceRecipe().getId()).isEqualTo(recipe.getId());
        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/recipes/" + candidateId);
    }

    @Test
    void passesPreviousCandidateToGeneratorWhenRerolling() throws Exception {
        Recipe previous = recipeRepository.save(Recipe.personalizationCandidate(
                recipe,
                "이전 닭가슴살 볶음",
                "이전 후보",
                20,
                new BigDecimal("190.00"),
                "이전 수정안",
                "이전 변경 이유",
                "resp_previous"
        ));
        recipeIngredientRepository.save(new RecipeIngredient(previous, chicken, BigDecimal.ONE));
        when(personalizationGenerator.generate(any())).thenAnswer(invocation -> {
            var context = invocation.getArgument(0,
                    com.glucobite.recipe.personalization.PersonalizationContext.class);
            assertThat(context.previousCandidate()).isNotNull();
            assertThat(context.previousCandidate().title()).isEqualTo("이전 닭가슴살 볶음");
            assertThat(context.previousCandidate().ingredients()).containsExactly("닭가슴살");
            return generatedCandidate(
                    "새로운 저탄수 수정안",
                    "콜리플라워 볶음",
                    cauliflower.getId(),
                    chicken.getId()
            );
        });

        mockMvc.perform(post("/api/recipes/{id}/personalized", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"previousCandidateId\":" + previous.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.candidateRecipeId").value(org.hamcrest.Matchers.not(
                        previous.getId().intValue()
                )))
                .andExpect(jsonPath("$.label").value("새로운 저탄수 수정안"));
    }

    @Test
    void alternativesExcludeCandidatesRestrictedByProfile() throws Exception {
        mockMvc.perform(get(
                        "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                        recipe.getId(), pork.getId()
                ).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alternatives.length()").value(1))
                .andExpect(jsonPath("$.alternatives[0].ingredientId").value(chicken.getId()));
    }

    @Test
    void roundedAlternativeAmountCanBeAppliedWithoutClientConversion() throws Exception {
        Ingredient potato = saveIngredient("감자", "77.00", "17.00", "2.00");
        Ingredient pumpkin = saveIngredient("단호박", "29.00", "7.00", "1.00");
        ingredientSubstituteRepository.save(new IngredientSubstitute(
                potato, pumpkin, new BigDecimal("0.3333"), "권장 사용량 반올림 테스트"
        ));
        recipeIngredientRepository.save(new RecipeIngredient(
                recipe, potato, new BigDecimal("1.23")
        ));

        MvcResult alternativesResult = mockMvc.perform(get(
                        "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                        recipe.getId(), potato.getId()
                ).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alternatives[0].recommendedAmount").value(0.41))
                .andReturn();
        JsonNode alternatives = objectMapper.readTree(
                alternativesResult.getResponse().getContentAsByteArray()
        );
        String recommendedAmount = alternatives.get("alternatives").get(0)
                .get("recommendedAmount").decimalValue().toPlainString();

        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(substitutionBody(
                                substitutionItem(potato.getId(), pumpkin.getId(), recommendedAmount)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedIngredients[0].substituteIngredient.amount")
                        .value(0.41));
    }

    @Test
    void rejectsApplyingCandidateRestrictedByProfile() throws Exception {
        String body = substitutionBody(
                substitutionItem(pork.getId(), milk.getId(), "1.00")
        );

        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SUBSTITUTE_INGREDIENT"));
    }

    @Test
    void previewsAllSubstitutionsTogetherWithoutUpdatingOriginalRecipe() throws Exception {
        String body = substitutionBody(
                substitutionItem(pork.getId(), chicken.getId(), "0.80"),
                substitutionItem(rice.getId(), cauliflower.getId(), "1.00")
        );

        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedIngredients.length()").value(2))
                .andExpect(jsonPath("$.changedIngredients[0].substituteIngredient.ingredientId")
                        .value(chicken.getId()))
                .andExpect(jsonPath("$.changedIngredients[1].substituteIngredient.ingredientId")
                        .value(cauliflower.getId()))
                .andExpect(jsonPath("$.personalizedNutrition.calories").value(157.0000))
                .andExpect(jsonPath("$.personalizedNutrition.carb").value(5.0000));

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients[*].ingredientId")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                pork.getId().intValue(), rice.getId().intValue()
                        )));
    }

    @Test
    void rejectsDuplicateOriginalIngredientInOnePreview() throws Exception {
        String body = substitutionBody(
                substitutionItem(pork.getId(), chicken.getId(), "0.80"),
                substitutionItem(pork.getId(), chicken.getId(), "0.90")
        );

        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECIPE_SUBSTITUTION"));
    }

    @Test
    void rejectsDuplicateIngredientInFinalResult() throws Exception {
        recipeIngredientRepository.save(new RecipeIngredient(recipe, chicken, BigDecimal.ONE));
        String body = substitutionBody(
                substitutionItem(pork.getId(), chicken.getId(), "0.80")
        );

        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RECIPE_SUBSTITUTION"));
    }

    @Test
    void rejectsEmptySubstitutionList() throws Exception {
        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"substitutions\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsAmountLargerThanRecipeIngredientColumn() throws Exception {
        String body = substitutionBody(
                substitutionItem(pork.getId(), chicken.getId(), "999999999.00")
        );

        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsNullSubstitutionItemInPreview() throws Exception {
        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"substitutions\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsNullSubstitutionItemWhenSaving() throws Exception {
        mockMvc.perform(post("/api/recipes/{id}/substitutions", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":null,\"substitutions\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void savesAllSubstitutionsAsNewCompletedRecipeAndKeepsOriginal() throws Exception {
        String body = saveSubstitutionBody(
                "닭가슴살 콜리플라워 볶음",
                substitutionItem(pork.getId(), chicken.getId(), "0.80"),
                substitutionItem(rice.getId(), cauliflower.getId(), "1.00")
        );

        MvcResult result = mockMvc.perform(post("/api/recipes/{id}/substitutions", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.sourceRecipeId").value(recipe.getId()))
                .andExpect(jsonPath("$.nutrition.calories").value(157.0000))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        Long savedRecipeId = response.get("recipeId").asLong();
        Recipe saved = recipeRepository.findById(savedRecipeId).orElseThrow();

        assertThat(result.getResponse().getHeader("Location"))
                .isEqualTo("/api/recipes/" + savedRecipeId);
        assertThat(saved.getTitle()).isEqualTo("닭가슴살 콜리플라워 볶음");
        assertThat(saved.isCompleted()).isTrue();
        assertThat(saved.getTotalCalories()).isEqualByComparingTo("157.00");
        assertThat(recipeIngredientRepository.findByRecipeId(savedRecipeId))
                .extracting(item -> item.getIngredient().getId())
                .containsExactlyInAnyOrder(chicken.getId(), cauliflower.getId());
        assertThat(recipeStepRepository.findByRecipeIdOrderByStepOrderAsc(savedRecipeId))
                .extracting(RecipeStep::getDescription)
                .containsExactly("재료를 손질한다.", "볶는다.");
        assertThat(recipeIngredientRepository.findByRecipeId(recipe.getId()))
                .extracting(item -> item.getIngredient().getId())
                .containsExactlyInAnyOrder(pork.getId(), rice.getId());
    }

    @Test
    void rollsBackSaveWhenAnySubstitutionIsInvalid() throws Exception {
        String body = saveSubstitutionBody(
                "저장되면 안 되는 레시피",
                substitutionItem(pork.getId(), chicken.getId(), "0.80"),
                substitutionItem(rice.getId(), milk.getId(), "1.00")
        );

        mockMvc.perform(post("/api/recipes/{id}/substitutions", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SUBSTITUTE_INGREDIENT"));

        assertThat(recipeRepository.count()).isEqualTo(1);
        assertThat(recipeIngredientRepository.findByRecipeId(recipe.getId()))
                .extracting(item -> item.getIngredient().getId())
                .containsExactlyInAnyOrder(pork.getId(), rice.getId());
    }

    @Test
    void usesOriginalTitleWhenSaveTitleIsMissing() throws Exception {
        String body = saveSubstitutionBody(
                null,
                substitutionItem(pork.getId(), chicken.getId(), "0.80")
        );

        mockMvc.perform(post("/api/recipes/{id}/substitutions", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(recipe.getTitle()));
    }

    @Test
    void documentsBatchPreviewAndSaveEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/substitutions/preview'].post.security[0].bearerAuth"
                ).isArray())
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/substitutions'].post.security[0].bearerAuth"
                ).isArray())
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/substitutions'].post.responses['201']"
                ).exists());
    }

    @Test
    void hidesPersonalizationEndpointsFromNonOwner() throws Exception {
        String token = bearer(anotherUser);
        String body = substitutionBody(
                substitutionItem(pork.getId(), chicken.getId(), "0.80")
        );

        mockMvc.perform(post("/api/recipes/{id}/personalized", recipe.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        mockMvc.perform(get(
                        "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                        recipe.getId(), pork.getId()
                ).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        mockMvc.perform(post("/api/recipes/{id}/substitutions/preview", recipe.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        mockMvc.perform(post("/api/recipes/{id}/substitutions", recipe.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveSubstitutionBody(
                                null,
                                substitutionItem(pork.getId(), chicken.getId(), "0.80")
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
    }

    @Test
    void personalizedDetailRequiresHealthProfile() throws Exception {
        Recipe otherRecipe = recipeRepository.save(new Recipe(
                anotherUser, "프로필 없는 레시피", null, 10
        ));
        recipeIngredientRepository.save(new RecipeIngredient(otherRecipe, pork, BigDecimal.ONE));

        mockMvc.perform(post("/api/recipes/{id}/personalized", otherRecipe.getId())
                        .header("Authorization", bearer(anotherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HEALTH_PROFILE_NOT_FOUND"));
    }

    @Test
    void rejectsGeneratedCandidateThatViolatesHealthRestrictions() throws Exception {
        User vegan = userRepository.save(new User("vegan-owner", "encoded-password", "비건"));
        saveProfile(vegan, VegetarianType.VEGAN, "돼지고기");
        Recipe veganRecipe = recipeRepository.save(new Recipe(vegan, "비건 대체 테스트", null, 10));
        recipeIngredientRepository.save(new RecipeIngredient(veganRecipe, pork, BigDecimal.ONE));
        when(personalizationGenerator.generate(any())).thenReturn(generatedCandidate(
                "잘못된 수정안", "돼지고기 유지", pork.getId(), rice.getId()
        ));

        mockMvc.perform(post("/api/recipes/{id}/personalized", veganRecipe.getId())
                        .header("Authorization", bearer(vegan))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code")
                        .value("RECIPE_PERSONALIZATION_GENERATION_FAILED"));
    }

    private GeneratedPersonalization generatedCandidate(
            String label,
            String title,
            Long firstIngredientId,
            Long secondIngredientId
    ) {
        return new GeneratedPersonalization(
                "resp_test",
                label,
                title,
                "건강 목표에 맞춘 후보",
                20,
                "탄수화물을 낮추고 단백질을 보강했습니다.",
                List.of(
                        new GeneratedPersonalization.IngredientAmount(
                                firstIngredientId, BigDecimal.ONE
                        ),
                        new GeneratedPersonalization.IngredientAmount(
                                secondIngredientId, BigDecimal.ONE
                        )
                ),
                List.of("재료를 손질한다.", "팬에서 볶는다.")
        );
    }

    private Ingredient saveIngredient(String title, String calories, String carb, String protein) {
        Ingredient ingredient = ingredientRepository.save(new Ingredient(title));
        ingredientNutritionRepository.save(new IngredientNutrition(
                ingredient, new BigDecimal(calories), new BigDecimal(carb),
                new BigDecimal(protein), BigDecimal.ZERO, BigDecimal.ZERO
        ));
        return ingredient;
    }

    private void saveProfile(User user, VegetarianType vegetarianType, String... allergenNames) {
        List<Allergen> allergens = List.of(allergenNames).stream()
                .map(name -> allergenRepository.findByName(name).orElseThrow())
                .toList();
        healthProfileRepository.save(new HealthProfile(
                user, LocalDate.of(1990, 1, 1), new BigDecimal("170.00"),
                new BigDecimal("65.00"), Sex.MALE, HealthGoal.CARB_MANAGEMENT,
                null, false, 180, vegetarianType, null, allergens
        ));
    }

    private String substitutionBody(String... items) {
        return """
                {
                  "substitutions": [%s]
                }
                """.formatted(String.join(",", items));
    }

    private String saveSubstitutionBody(String title, String... items) {
        String jsonTitle = title == null ? "null" : "\"" + title + "\"";
        return """
                {
                  "title": %s,
                  "substitutions": [%s]
                }
                """.formatted(jsonTitle, String.join(",", items));
    }

    private String substitutionItem(Long originalId, Long substituteId, String amount) {
        return """
                {
                  "originalIngredientId": %d,
                  "substituteIngredientId": %d,
                  "amount": %s
                }
                """.formatted(originalId, substituteId, amount);
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM meal_logs");
        jdbcTemplate.update("DELETE FROM recipe_ingredients");
        jdbcTemplate.update("DELETE FROM recipe_steps");
        jdbcTemplate.update("DELETE FROM ingredient_substitutes");
        jdbcTemplate.update("DELETE FROM ingredient_nutritions");
        jdbcTemplate.update("DELETE FROM recipes");
        jdbcTemplate.update("DELETE FROM ingredients");
        jdbcTemplate.update("DELETE FROM health_profile_allergies");
        jdbcTemplate.update("DELETE FROM health_profiles");
        jdbcTemplate.update("DELETE FROM users");
    }
}
