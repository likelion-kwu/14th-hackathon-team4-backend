package com.glucobite.meal.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientRepository;
import com.glucobite.meal.repository.MealLogRepository;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class MealLogControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private RecipeIngredientRepository recipeIngredientRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IngredientNutritionRepository nutritionRepository;
    @Autowired private MealLogRepository mealLogRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        mealLogRepository.deleteAll();
        recipeIngredientRepository.deleteAll();
        nutritionRepository.deleteAll();
        ingredientRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void recordsManualMealAndReturnsItByDate() throws Exception {
        User user = createUser("manual-meal-user");
        String eatenAt = LocalDateTime.now().minusHours(1).withNano(0).toString();

        mockMvc.perform(post("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"직접 만든 샐러드",
                                  "mealType":"LUNCH",
                                  "calories":320.5,
                                  "carb":28.4,
                                  "sugar":6.2,
                                  "eatenAt":"%s"
                                }
                                """.formatted(eatenAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipeId").doesNotExist())
                .andExpect(jsonPath("$.title").value("직접 만든 샐러드"))
                .andExpect(jsonPath("$.carb").value(28.4));

        String today = LocalDate.now().toString();
        mockMvc.perform(get("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .param("from", today)
                        .param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals.length()").value(1))
                .andExpect(jsonPath("$.meals[0].title").value("직접 만든 샐러드"));
    }

    @Test
    void snapshotsOwnedRecipeNutritionAtRecordingTime() throws Exception {
        User user = createUser("recipe-meal-user");
        Recipe recipe = createRecipe(user, "현미 덮밥");
        String eatenAt = LocalDateTime.now().minusMinutes(10).withNano(0).toString();

        mockMvc.perform(post("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipeId":%d,"mealType":"DINNER","eatenAt":"%s"}
                                """.formatted(recipe.getId(), eatenAt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("현미 덮밥"))
                .andExpect(jsonPath("$.calories").value(200.0))
                .andExpect(jsonPath("$.carb").value(20.0))
                .andExpect(jsonPath("$.sugar").value(5.0));

        jdbcTemplate.update("UPDATE ingredient_nutritions SET carb = 9.0, sugar = 9.0");

        mockMvc.perform(get("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meals[0].carb").value(20.0))
                .andExpect(jsonPath("$.meals[0].sugar").value(5.0));
    }

    @Test
    void rejectsOtherUsersRecipeAndAmbiguousSource() throws Exception {
        User owner = createUser("recipe-owner");
        User attacker = createUser("recipe-attacker");
        Recipe recipe = createRecipe(owner, "소유자 레시피");
        String eatenAt = LocalDateTime.now().minusMinutes(1).withNano(0).toString();

        mockMvc.perform(post("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipeId":%d,"mealType":"LUNCH","eatenAt":"%s"}
                                """.formatted(recipe.getId(), eatenAt)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipeId":%d,"title":"중복 입력","mealType":"LUNCH",
                                  "calories":1,"carb":1,"sugar":1,"eatenAt":"%s"
                                }
                                """.formatted(recipe.getId(), eatenAt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsPartialOrOversizedDateRange() throws Exception {
        User user = createUser("range-user");

        mockMvc.perform(get("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .param("from", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
        mockMvc.perform(get("/api/meal-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user))
                        .param("from", "2026-01-01")
                        .param("to", "2026-02-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    private User createUser(String loginId) {
        return userRepository.save(new User(loginId, "hash", "기록 사용자"));
    }

    private Recipe createRecipe(User user, String title) {
        Recipe recipe = recipeRepository.save(new Recipe(
                user, title, null, 10, null, new BigDecimal("999.00")
        ));
        Ingredient ingredient = ingredientRepository.save(new Ingredient(title + " 재료"));
        nutritionRepository.save(new IngredientNutrition(
                ingredient,
                new BigDecimal("2.0"),
                new BigDecimal("0.2"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("0.05"),
                BigDecimal.ZERO
        ));
        recipeIngredientRepository.save(new RecipeIngredient(
                recipe, ingredient, new BigDecimal("100")
        ));
        return recipe;
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }
}
