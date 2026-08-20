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
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User owner;
    private User anotherUser;
    private Recipe recipe;
    private Ingredient pork;
    private Ingredient milk;
    private Ingredient chicken;

    @BeforeEach
    void setUp() {
        cleanUp();
        pork = saveIngredient("돼지고기", "250.00", "20.00");
        milk = saveIngredient("우유", "60.00", "3.00");
        chicken = saveIngredient("닭가슴살", "165.00", "31.00");

        ingredientSubstituteRepository.save(new IngredientSubstitute(
                pork, milk, new BigDecimal("1.0000"), "첫 번째지만 알레르기 후보"
        ));
        ingredientSubstituteRepository.save(new IngredientSubstitute(
                pork, chicken, new BigDecimal("0.8000"), "안전한 저지방 후보"
        ));

        owner = userRepository.save(new User("personal-owner", "encoded-password", "소유자"));
        anotherUser = userRepository.save(new User("personal-other", "encoded-password", "다른 사용자"));
        saveProfile(owner, VegetarianType.NONE, "돼지고기", "우유");

        recipe = recipeRepository.save(new Recipe(owner, "돼지고기 볶음", null, 20));
        recipeIngredientRepository.save(new RecipeIngredient(recipe, pork, BigDecimal.ONE));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void autoSubstitutionSkipsAnotherAllergenAndUsesSafeCandidate() throws Exception {
        mockMvc.perform(get("/api/recipes/{id}/personalized", recipe.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(chicken.getId()))
                .andExpect(jsonPath("$.ingredients[0].changed").value(true))
                .andExpect(jsonPath("$.ingredients[0].changeReason").value("안전한 저지방 후보"));
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
    void rejectsApplyingCandidateRestrictedByProfile() throws Exception {
        String body = substituteBody(pork.getId(), milk.getId(), "1.00");

        mockMvc.perform(post("/api/recipes/{id}/ingredients/substitute", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SUBSTITUTE_INGREDIENT"));
    }

    @Test
    void appliesSafeSubstituteWithoutUpdatingOriginalRecipe() throws Exception {
        String body = substituteBody(pork.getId(), chicken.getId(), "0.80");

        mockMvc.perform(post("/api/recipes/{id}/ingredients/substitute", recipe.getId())
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedIngredient.substituteIngredient.ingredientId")
                        .value(chicken.getId()))
                .andExpect(jsonPath("$.nutrition.calories").value(132.0000));

        mockMvc.perform(get("/api/recipes/{id}", recipe.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(pork.getId()));
    }

    @Test
    void hidesPersonalizationEndpointsFromNonOwner() throws Exception {
        String token = bearer(anotherUser);
        String body = substituteBody(pork.getId(), chicken.getId(), "0.80");

        mockMvc.perform(get("/api/recipes/{id}/personalized", recipe.getId())
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        mockMvc.perform(get(
                        "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                        recipe.getId(), pork.getId()
                ).header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        mockMvc.perform(post("/api/recipes/{id}/ingredients/substitute", recipe.getId())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
    }

    @Test
    void personalizedDetailRequiresHealthProfile() throws Exception {
        Recipe otherRecipe = recipeRepository.save(new Recipe(
                anotherUser, "프로필 없는 레시피", null, 10
        ));
        recipeIngredientRepository.save(new RecipeIngredient(otherRecipe, pork, BigDecimal.ONE));

        mockMvc.perform(get("/api/recipes/{id}/personalized", otherRecipe.getId())
                        .header("Authorization", bearer(anotherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HEALTH_PROFILE_NOT_FOUND"));
    }

    @Test
    void leavesOriginalWhenVegetarianProfileHasNoSafeCandidate() throws Exception {
        User vegan = userRepository.save(new User("vegan-owner", "encoded-password", "비건"));
        saveProfile(vegan, VegetarianType.VEGAN, "돼지고기");
        Recipe veganRecipe = recipeRepository.save(new Recipe(vegan, "비건 대체 테스트", null, 10));
        recipeIngredientRepository.save(new RecipeIngredient(veganRecipe, pork, BigDecimal.ONE));

        mockMvc.perform(get("/api/recipes/{id}/personalized", veganRecipe.getId())
                        .header("Authorization", bearer(vegan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(pork.getId()))
                .andExpect(jsonPath("$.ingredients[0].changed").value(false));
    }

    private Ingredient saveIngredient(String title, String calories, String protein) {
        Ingredient ingredient = ingredientRepository.save(new Ingredient(title));
        ingredientNutritionRepository.save(new IngredientNutrition(
                ingredient, new BigDecimal(calories), BigDecimal.ZERO,
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

    private String substituteBody(Long originalId, Long substituteId, String amount) {
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
