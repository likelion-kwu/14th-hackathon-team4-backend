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
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientRepository;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.entity.RecipeStep;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeReadIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HealthProfileRepository healthProfileRepository;
    @Autowired private AllergenRepository allergenRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private RecipeStepRepository recipeStepRepository;
    @Autowired private RecipeIngredientRepository recipeIngredientRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IngredientNutritionRepository ingredientNutritionRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private User owner;
    private User anotherUser;
    private Recipe safeRecipe;
    private Recipe allergenRecipe;
    private Ingredient tofu;
    private Ingredient pork;

    @BeforeEach
    void setUp() {
        cleanUp();
        tofu = saveIngredient("두부", "80.00", "2.00");
        pork = saveIngredient("돼지고기", "250.00", "0.00");
        owner = userRepository.save(new User("read-owner", "encoded-password", "소유자"));
        anotherUser = userRepository.save(new User("read-other", "encoded-password", "다른 사용자"));

        Allergen porkAllergen = allergenRepository.findByName("돼지고기").orElseThrow();
        healthProfileRepository.save(new HealthProfile(
                owner, LocalDate.of(1990, 1, 1), new BigDecimal("170.00"),
                new BigDecimal("65.00"), Sex.MALE, HealthGoal.CARB_MANAGEMENT,
                null, false, 180, VegetarianType.NONE, null, List.of(porkAllergen)
        ));

        safeRecipe = saveRecipe(owner, "두부구이", tofu, "2.00");
        recipeStepRepository.save(new RecipeStep(safeRecipe, 2, "굽는다."));
        recipeStepRepository.save(new RecipeStep(safeRecipe, 1, "두부를 썬다."));
        allergenRecipe = saveRecipe(owner, "돼지고기 볶음", pork, "1.00");
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void returnsOwnedRecipeDetailAndOrderedSteps() throws Exception {
        mockMvc.perform(get("/api/recipes/{id}", safeRecipe.getId())
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(safeRecipe.getId()))
                .andExpect(jsonPath("$.nutrition.calories").value(160.00))
                .andExpect(jsonPath("$.steps[0].stepOrder").value(1))
                .andExpect(jsonPath("$.steps[1].stepOrder").value(2));
    }

    @Test
    void hidesRecipeFromNonOwnerAcrossDetailAndSteps() throws Exception {
        mockMvc.perform(get("/api/recipes/{id}", safeRecipe.getId())
                        .header("Authorization", bearer(anotherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        mockMvc.perform(get("/api/recipes/{id}/steps", safeRecipe.getId())
                        .header("Authorization", bearer(anotherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
    }

    @Test
    void returnsSafeRecipeEvenWhenTwentyNewerRecipesAreFilteredOut() throws Exception {
        for (int index = 0; index < 20; index++) {
            saveRecipe(owner, "알레르기 레시피 " + index, pork, "1.00");
        }

        mockMvc.perform(get("/api/recipes/recommendations")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(1))
                .andExpect(jsonPath("$.recommendations[0].recipeId").value(safeRecipe.getId()));
    }

    @Test
    void excludesRecipeAboveDailyCarbsTarget() throws Exception {
        Ingredient rice = saveIngredient("쌀", "300.00", "100.00");
        Recipe highCarbRecipe = saveRecipe(owner, "고탄수 레시피", rice, "2.00");

        mockMvc.perform(get("/api/recipes/recommendations")
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations[*].recipeId",
                        not(hasItem(highCarbRecipe.getId().intValue()))));
    }

    @Test
    void recommendationsRequireHealthProfile() throws Exception {
        mockMvc.perform(get("/api/recipes/recommendations")
                        .header("Authorization", bearer(anotherUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HEALTH_PROFILE_NOT_FOUND"));
    }

    @Test
    void documentsBearerSecurityForRecipeEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/recipes/{recipeId}'].get.security[0].bearerAuth")
                        .isArray())
                .andExpect(jsonPath("$.paths['/api/recipes/recommendations'].get.security[0].bearerAuth")
                        .isArray());
    }

    private Ingredient saveIngredient(String title, String calories, String carb) {
        Ingredient ingredient = ingredientRepository.save(new Ingredient(title));
        ingredientNutritionRepository.save(new IngredientNutrition(
                ingredient, new BigDecimal(calories), new BigDecimal(carb),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        ));
        return ingredient;
    }

    private Recipe saveRecipe(User user, String title, Ingredient ingredient, String amount) {
        Recipe recipe = recipeRepository.save(new Recipe(user, title, null, 20));
        recipeIngredientRepository.save(new RecipeIngredient(
                recipe, ingredient, new BigDecimal(amount)
        ));
        return recipe;
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
