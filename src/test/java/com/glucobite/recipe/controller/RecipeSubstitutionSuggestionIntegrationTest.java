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
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.entity.Recipe;
import com.glucobite.recipe.entity.RecipeIngredient;
import com.glucobite.recipe.entity.RecipeStep;
import com.glucobite.recipe.repository.RecipeIngredientRepository;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.repository.RecipeStepRepository;
import com.glucobite.recipe.repository.RecipeSubstitutionSuggestionRepository;
import com.glucobite.recipe.repository.RecipeSubstitutionSuggestionSourceRepository;
import com.glucobite.recipe.substitution.GeneratedSubstitutionSuggestions;
import com.glucobite.recipe.substitution.RecipeSubstitutionSuggestionGenerator;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecipeSubstitutionSuggestionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private HealthProfileRepository healthProfileRepository;
    @Autowired private AllergenRepository allergenRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IngredientNutritionRepository ingredientNutritionRepository;
    @Autowired private IngredientSubstituteRepository ingredientSubstituteRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private RecipeIngredientRepository recipeIngredientRepository;
    @Autowired private RecipeStepRepository recipeStepRepository;
    @Autowired private RecipeSubstitutionSuggestionRepository suggestionRepository;
    @Autowired private RecipeSubstitutionSuggestionSourceRepository sourceRepository;
    @MockitoBean private RecipeSubstitutionSuggestionGenerator suggestionGenerator;

    private User owner;
    private User anotherUser;
    private Recipe recipe;
    private Ingredient pork;
    private Ingredient chicken;

    @BeforeEach
    void setUp() {
        cleanUp();
        pork = saveIngredient("돼지고기", "2.500000", "0.000000", "0.200000");
        chicken = saveIngredient("닭가슴살", "1.650000", "0.000000", "0.310000");
        owner = userRepository.save(new User("suggest-owner", "encoded-password", "소유자"));
        anotherUser = userRepository.save(new User("suggest-other", "encoded-password", "다른 사용자"));
        saveProfile(owner, List.of());
        saveProfile(anotherUser, List.of());
        recipe = recipeRepository.save(new Recipe(owner, "돼지고기 볶음", "팬 볶음", 20));
        recipeIngredientRepository.save(new RecipeIngredient(recipe, pork, new BigDecimal("100.00")));
        recipeStepRepository.save(new RecipeStep(recipe, 1, "돼지고기를 팬에서 볶는다."));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void generatesRecipeScopedSuggestionsWithSourcesAndNutritionSnapshots() throws Exception {
        when(suggestionGenerator.generate(any())).thenAnswer(invocation -> {
            var context = invocation.getArgument(
                    0,
                    com.glucobite.recipe.substitution.SubstitutionSuggestionContext.class
            );
            assertThat(context.userInput()).isEqualTo("집에 있는 두부로 바꾸고 싶어");
            assertThat(context.recipe().title()).isEqualTo("돼지고기 볶음");
            assertThat(context.originalIngredient().title()).isEqualTo("돼지고기");
            assertThat(context.health().healthGoal()).isEqualTo("CARB_MANAGEMENT");
            return generated("두부", "0.760000", "0.081000");
        });

        var result = mockMvc.perform(post(
                                "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                                recipe.getId(),
                                pork.getId()
                        )
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userInput": "  집에 있는   두부로 바꾸고 싶어  ",
                                  "excludeSuggestionIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(recipe.getId()))
                .andExpect(jsonPath("$.originalIngredient.ingredientId").value(pork.getId()))
                .andExpect(jsonPath("$.suggestions.length()").value(1))
                .andExpect(jsonPath("$.suggestions[0].origin").value("AI_WEB_SEARCH"))
                .andExpect(jsonPath("$.suggestions[0].title").value("두부"))
                .andExpect(jsonPath("$.suggestions[0].recommendedAmount").value(180.00))
                .andExpect(jsonPath("$.suggestions[0].sources[0].url")
                        .value("https://example.com/tofu"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        Long suggestionId = response.get("suggestions").get(0).get("suggestionId").asLong();
        var saved = suggestionRepository.findById(suggestionId).orElseThrow();
        assertThat(saved.getUser().getId()).isEqualTo(owner.getId());
        assertThat(saved.getRecipe().getId()).isEqualTo(recipe.getId());
        assertThat(saved.getCaloriesPerGram()).isEqualByComparingTo("0.760000");
        assertThat(sourceRepository.count()).isEqualTo(1);
        assertThat(ingredientNutritionRepository
                .findByIngredientId(saved.getSubstituteIngredient().getId())).isEmpty();
        assertThat(ingredientSubstituteRepository.findAll()).isEmpty();
    }

    @Test
    void returnsCachedSuggestionsWithoutCallingOpenAIAgain() throws Exception {
        when(suggestionGenerator.generate(any())).thenReturn(
                generated("두부", "0.760000", "0.081000")
        );
        String body = """
                {"userInput":"두부 말고 담백한 재료","excludeSuggestionIds":[]}
                """;

        String first = mockMvc.perform(post(
                                "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                                recipe.getId(),
                                pork.getId()
                        )
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post(
                                "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                                recipe.getId(),
                                pork.getId()
                        )
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(first).get("suggestions").get(0).get("suggestionId").asLong())
                .isEqualTo(objectMapper.readTree(second)
                        .get("suggestions").get(0).get("suggestionId").asLong());
        verify(suggestionGenerator, times(1)).generate(any());
    }

    @Test
    void returnsMatchingRegisteredSubstituteWithoutCallingOpenAI() throws Exception {
        ingredientSubstituteRepository.save(new IngredientSubstitute(
                pork,
                chicken,
                new BigDecimal("0.8000"),
                "지방을 줄이는 등록 후보"
        ));

        mockMvc.perform(post(
                                "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                                recipe.getId(),
                                pork.getId()
                        )
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"userInput\":\"집에 닭가슴살이 있어\"" + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].origin").value("REGISTERED"))
                .andExpect(jsonPath("$.suggestions[0].suggestionId").doesNotExist())
                .andExpect(jsonPath("$.suggestions[0].substituteIngredientId")
                        .value(chicken.getId()))
                .andExpect(jsonPath("$.suggestions[0].recommendedAmount").value(80.00));

        verify(suggestionGenerator, never()).generate(any());
        assertThat(suggestionRepository.count()).isZero();
    }

    @Test
    void rejectsGeneratedSuggestionThatViolatesAllergenRestriction() throws Exception {
        Allergen soybean = allergenRepository.findByName("대두").orElseThrow();
        healthProfileRepository.deleteById(
                healthProfileRepository.findByUserId(owner.getId()).orElseThrow().getId()
        );
        saveProfile(owner, List.of(soybean));
        when(suggestionGenerator.generate(any())).thenReturn(
                generated("두부", "0.760000", "0.081000")
        );

        mockMvc.perform(post(
                                "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                                recipe.getId(),
                                pork.getId()
                        )
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"userInput\":\"다른 단백질\"" + "}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code")
                        .value("RECIPE_PERSONALIZATION_GENERATION_FAILED"));

        assertThat(suggestionRepository.count()).isZero();
        assertThat(ingredientRepository.findByNormalizedTitle("두부")).isEmpty();
    }

    @Test
    void hidesRecipeFromNonOwnerAndValidatesBlankInput() throws Exception {
        mockMvc.perform(post(
                                "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                                recipe.getId(),
                                pork.getId()
                        )
                        .header("Authorization", bearer(anotherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"userInput\":\"두부\"" + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        mockMvc.perform(post(
                                "/api/recipes/{recipeId}/ingredients/{ingredientId}/alternatives",
                                recipe.getId(),
                                pork.getId()
                        )
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"userInput\":\"   \"" + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private GeneratedSubstitutionSuggestions generated(
            String title,
            String caloriesPerGram,
            String proteinPerGram
    ) {
        return new GeneratedSubstitutionSuggestions(
                "resp_suggestion",
                List.of(new GeneratedSubstitutionSuggestions.Suggestion(
                        title,
                        new BigDecimal("180.00"),
                        "단백질을 유지하면서 포화지방을 줄일 수 있습니다.",
                        "수분을 제거한 뒤 사용하세요.",
                        new NutritionSummary(
                                new BigDecimal(caloriesPerGram),
                                new BigDecimal("0.019000"),
                                new BigDecimal(proteinPerGram),
                                new BigDecimal("0.042000"),
                                new BigDecimal("0.003000"),
                                new BigDecimal("0.007000"),
                                new BigDecimal("0.070000")
                        )
                )),
                List.of(new GeneratedSubstitutionSuggestions.Source(
                        "두부 영양정보",
                        "https://example.com/tofu"
                ))
        );
    }

    private Ingredient saveIngredient(String title, String calories, String carb, String protein) {
        Ingredient ingredient = ingredientRepository.save(new Ingredient(title));
        ingredientNutritionRepository.save(new IngredientNutrition(
                ingredient,
                new BigDecimal(calories),
                new BigDecimal(carb),
                new BigDecimal(protein),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
        return ingredient;
    }

    private void saveProfile(User user, List<Allergen> allergens) {
        healthProfileRepository.save(new HealthProfile(
                user,
                LocalDate.of(1990, 1, 1),
                new BigDecimal("170.00"),
                new BigDecimal("65.00"),
                Sex.MALE,
                HealthGoal.CARB_MANAGEMENT,
                null,
                false,
                180,
                VegetarianType.NONE,
                null,
                allergens
        ));
    }

    private String bearer(User user) {
        return "Bearer " + jwtTokenService.issue(user.getId()).accessToken();
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM recipe_substitution_suggestion_sources");
        jdbcTemplate.update("DELETE FROM recipe_substitution_suggestions");
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
