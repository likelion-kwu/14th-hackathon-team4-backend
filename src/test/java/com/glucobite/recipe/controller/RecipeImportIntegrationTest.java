package com.glucobite.recipe.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.ingredient.entity.Ingredient;
import com.glucobite.ingredient.entity.IngredientNutrition;
import com.glucobite.ingredient.repository.IngredientNutritionRepository;
import com.glucobite.ingredient.repository.IngredientRepository;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.entity.RecipeType;
import com.glucobite.recipe.exception.InvalidRecipeAnalysisException;
import com.glucobite.recipe.exception.RecipeImportGenerationException;
import com.glucobite.recipe.importing.AnalyzedRecipe;
import com.glucobite.recipe.importing.RecipeTextAnalyzer;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeImportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private RecipeIngredientRepository recipeIngredientRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IngredientNutritionRepository ingredientNutritionRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private RecipeTextAnalyzer recipeTextAnalyzer;
    @MockitoSpyBean private RecipeStepRepository recipeStepRepository;

    private User user;

    @BeforeEach
    void setUp() {
        reset(recipeTextAnalyzer, recipeStepRepository);
        cleanUp();
        user = userRepository.save(new User("recipe-import", "encoded-password", "불러오기"));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void importsAnalyzedTextAsIncompleteBaseRecipe() throws Exception {
        given(recipeTextAnalyzer.analyze("계란 볶음밥 레시피"))
                .willReturn(validAnalysis());

        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("""
                                {"text":"계란 볶음밥 레시피"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/recipes/")))
                .andExpect(jsonPath("$.title").value("계란 볶음밥"))
                .andExpect(jsonPath("$.importType").value("TEXT"))
                .andExpect(jsonPath("$.recipeType").value("BASE"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.nutrition.calories").value(205.00))
                .andExpect(jsonPath("$.nutrition.carb").value(28.50))
                .andExpect(jsonPath("$.nutrition.protein").value(9.50))
                .andExpect(jsonPath("$.nutrition.sugar").value(0.50))
                .andExpect(jsonPath("$.nutrition.sodium").value(62.00))
                .andExpect(jsonPath("$.ingredients.length()").value(2))
                .andExpect(jsonPath("$.steps[0].stepOrder").value(1))
                .andExpect(jsonPath("$.steps[1].stepOrder").value(2));

        var saved = recipeRepository.findAll().getFirst();
        assertThat(saved.getRecipeType()).isEqualTo(RecipeType.BASE);
        assertThat(saved.getImportType()).isEqualTo(RecipeImportType.TEXT);
        assertThat(saved.isCompleted()).isFalse();
        assertThat(recipeIngredientRepository.findByRecipeId(saved.getId())).hasSize(2);
        assertThat(recipeStepRepository.findByRecipeIdOrderByStepOrderAsc(saved.getId()))
                .extracting(step -> step.getStepOrder())
                .containsExactly(1, 2);
    }

    @Test
    void reusesExistingIngredientNutritionInsteadOfGptEstimate() throws Exception {
        Ingredient rice = ingredientRepository.save(new Ingredient("쌀"));
        ingredientNutritionRepository.save(new IngredientNutrition(
                rice,
                new BigDecimal("2.00"),
                new BigDecimal("0.50"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
        given(recipeTextAnalyzer.analyze(any())).willReturn(new AnalyzedRecipe(
                "밥", null, 20,
                List.of(ingredient("쌀", "100", "1.30", "0.28", "0", "0", "0", "0", "0")),
                List.of("밥을 짓는다.")
        ));

        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"밥 짓기\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nutrition.calories").value(200.00))
                .andExpect(jsonPath("$.nutrition.carb").value(50.00));

        assertThat(ingredientRepository.count()).isEqualTo(1);
        assertThat(ingredientNutritionRepository.count()).isEqualTo(1);
    }

    @Test
    void mergesDuplicateIngredientNamesBeforeSaving() throws Exception {
        given(recipeTextAnalyzer.analyze(any())).willReturn(new AnalyzedRecipe(
                "두부 요리", null, 10,
                List.of(
                        ingredient("두부", "100", "0.80", "0.02", "0.08", "0.05", "0", "0", "0"),
                        ingredient("  두부  ", "50", "0.80", "0.02", "0.08", "0.05", "0", "0", "0")
                ),
                List.of("두부를 굽는다.")
        ));

        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"두부 요리\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ingredients.length()").value(1))
                .andExpect(jsonPath("$.ingredients[0].amount").value(150.00));
    }

    @Test
    void rejectsBlankAndOversizedTextBeforeCallingOpenAi() throws Exception {
        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"" + "가".repeat(50_001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(recipeTextAnalyzer);
    }

    @Test
    void mapsNonRecipeAndOpenAiFailures() throws Exception {
        given(recipeTextAnalyzer.analyze("일기"))
                .willThrow(new InvalidRecipeAnalysisException("입력에서 레시피를 찾지 못했습니다."));
        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"일기\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_RECIPE_ANALYSIS"));

        given(recipeTextAnalyzer.analyze("외부 오류"))
                .willThrow(new RecipeImportGenerationException("OpenAI 레시피 분석에 실패했습니다.",
                        new RuntimeException("timeout")));
        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"외부 오류\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("RECIPE_IMPORT_GENERATION_FAILED"));

        assertThat(recipeRepository.count()).isZero();
    }

    @Test
    void rejectsMalformedAnalysisWithoutPartialPersistence() throws Exception {
        given(recipeTextAnalyzer.analyze(any())).willReturn(new AnalyzedRecipe(
                "잘못된 레시피", null, 10,
                List.of(ingredient("소금", "-1", "0", "0", "0", "0", "0", "0", "0")),
                List.of("간을 한다.")
        ));

        mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"잘못된 입력\"}"))
                .andExpect(status().isUnprocessableEntity());

        assertThat(recipeRepository.count()).isZero();
        assertThat(ingredientRepository.count()).isZero();
    }

    @Test
    void rollsBackAllEntitiesWhenStepPersistenceFails() {
        given(recipeTextAnalyzer.analyze(any())).willReturn(validAnalysis());
        doThrow(new IllegalStateException("step save failed"))
                .when(recipeStepRepository).saveAll(any());

        assertThatThrownBy(() -> mockMvc.perform(post("/api/recipes/import/text")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"text\":\"계란 볶음밥 레시피\"}")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseMessage("step save failed");

        assertThat(recipeRepository.count()).isZero();
        assertThat(ingredientRepository.count()).isZero();
        assertThat(ingredientNutritionRepository.count()).isZero();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/recipes/import/text")
                        .contentType("application/json")
                        .content("{\"text\":\"계란 볶음밥\"}"))
                .andExpect(status().isUnauthorized());
    }

    private AnalyzedRecipe validAnalysis() {
        return new AnalyzedRecipe(
                "계란 볶음밥",
                "간단한 한 끼",
                20,
                List.of(
                        ingredient("쌀", "100", "1.30", "0.28", "0.03", "0", "0.01", "0", "0"),
                        ingredient("계란", "50", "1.50", "0.01", "0.13", "0.10", "0", "0.01", "1.24")
                ),
                List.of("재료를 손질한다.", "팬에서 볶는다.")
        );
    }

    private AnalyzedRecipe.IngredientData ingredient(
            String title,
            String amount,
            String calories,
            String carb,
            String protein,
            String fat,
            String fiber,
            String sugar,
            String sodium
    ) {
        return new AnalyzedRecipe.IngredientData(
                title,
                new BigDecimal(amount),
                new NutritionSummary(
                        new BigDecimal(calories),
                        new BigDecimal(carb),
                        new BigDecimal(protein),
                        new BigDecimal(fat),
                        new BigDecimal(fiber),
                        new BigDecimal(sugar),
                        new BigDecimal(sodium)
                )
        );
    }

    private String bearer() {
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
