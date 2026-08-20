package com.glucobite.recipe.controller;

import com.glucobite.common.security.JwtTokenService;
import com.glucobite.recipe.dto.NutritionSummary;
import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.exception.YouTubeFetchException;
import com.glucobite.recipe.exception.YouTubeTranscriptUnavailableException;
import com.glucobite.recipe.importing.AnalyzedRecipe;
import com.glucobite.recipe.importing.RecipeImportResult;
import com.glucobite.recipe.importing.RecipeSourceMetadata;
import com.glucobite.recipe.importing.RecipeTextAnalyzer;
import com.glucobite.recipe.repository.RecipeRepository;
import com.glucobite.recipe.service.RecipeImportService;
import com.glucobite.recipe.youtube.YouTubeTranscriptProvider;
import com.glucobite.recipe.youtube.YouTubeVideoContent;
import com.glucobite.recipe.youtube.YouTubeVideoReference;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class YouTubeRecipeImportIntegrationTest {

    private static final String VIDEO_ID = "dQw4w9WgXcQ";
    private static final String CANONICAL_URL =
            "https://www.youtube.com/watch?v=" + VIDEO_ID;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RecipeImportService recipeImportService;

    @MockitoBean private YouTubeTranscriptProvider transcriptProvider;
    @MockitoBean private RecipeTextAnalyzer recipeTextAnalyzer;

    private User user;

    @BeforeEach
    void setUp() {
        cleanUp();
        user = userRepository.save(new User("youtube-import", "encoded-password", "유튜브"));
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void importsYouTubeTranscriptWithSourceMetadata() throws Exception {
        given(transcriptProvider.fetch(new YouTubeVideoReference(VIDEO_ID, CANONICAL_URL)))
                .willReturn(videoContent());
        given(recipeTextAnalyzer.analyze(any())).willReturn(validAnalysis());

        mockMvc.perform(post("/api/recipes/import/youtube")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"url\":\"https://youtu.be/" + VIDEO_ID + "?t=10\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importType").value("URL"))
                .andExpect(jsonPath("$.recipeType").value("BASE"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.sourceUrl").value(CANONICAL_URL))
                .andExpect(jsonPath("$.sourceExternalId").value(VIDEO_ID))
                .andExpect(jsonPath("$.imageUrl").value("https://i.ytimg.com/test.jpg"));

        var saved = recipeRepository.findAll().getFirst();
        assertThat(saved.getImportType()).isEqualTo(RecipeImportType.URL);
        assertThat(saved.getSourceUrl()).isEqualTo(CANONICAL_URL);
        assertThat(saved.getSourceExternalId()).isEqualTo(VIDEO_ID);
        assertThat(saved.getImageUrl()).isEqualTo("https://i.ytimg.com/test.jpg");
        verify(recipeTextAnalyzer).analyze(
                "영상 제목: 계란 볶음밥 영상\n자막:\n계란과 밥을 팬에서 볶습니다."
        );
    }

    @Test
    void rejectsUnsafeUrlBeforeExternalCalls() throws Exception {
        mockMvc.perform(post("/api/recipes/import/youtube")
                        .header("Authorization", bearer())
                        .contentType("application/json")
                        .content("{\"url\":\"https://youtube.com.evil.example/watch?v="
                                + VIDEO_ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_YOUTUBE_URL"));

        verifyNoInteractions(transcriptProvider, recipeTextAnalyzer);
    }

    @Test
    void mapsUnavailableTranscriptAndUpstreamFailure() throws Exception {
        YouTubeVideoReference reference = new YouTubeVideoReference(VIDEO_ID, CANONICAL_URL);
        willThrow(new YouTubeTranscriptUnavailableException("사용 가능한 자막이 없습니다."))
                .given(transcriptProvider).fetch(reference);

        performImport()
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("YOUTUBE_TRANSCRIPT_UNAVAILABLE"));

        willThrow(new YouTubeFetchException("YouTube 요청에 실패했습니다."))
                .given(transcriptProvider).fetch(reference);

        performImport()
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("YOUTUBE_FETCH_FAILED"));

        assertThat(recipeRepository.count()).isZero();
        verifyNoInteractions(recipeTextAnalyzer);
    }

    @Test
    void returnsExistingRecipeWithoutRepeatingExternalCalls() throws Exception {
        given(transcriptProvider.fetch(any())).willReturn(videoContent());
        given(recipeTextAnalyzer.analyze(any())).willReturn(validAnalysis());

        performImport().andExpect(status().isCreated());
        Long recipeId = recipeRepository.findAll().getFirst().getId();
        performImport()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(recipeId));

        assertThat(recipeRepository.count()).isEqualTo(1);
        assertThat(recipeRepository.findAll().getFirst().getImportDedupeKey())
                .isEqualTo("YOUTUBE:" + VIDEO_ID);
        verify(transcriptProvider, times(1)).fetch(any());
        verify(recipeTextAnalyzer, times(1)).analyze(any());
    }

    @Test
    void allowsDifferentUsersToImportSameVideo() throws Exception {
        User otherUser = userRepository.save(new User(
                "youtube-import-other",
                "encoded-password",
                "다른 유튜브 사용자"
        ));
        given(transcriptProvider.fetch(any())).willReturn(videoContent());
        given(recipeTextAnalyzer.analyze(any())).willReturn(validAnalysis());

        performImport().andExpect(status().isCreated());
        mockMvc.perform(post("/api/recipes/import/youtube")
                        .header("Authorization", "Bearer "
                                + jwtTokenService.issue(otherUser.getId()).accessToken())
                        .contentType("application/json")
                        .content("{\"url\":\"" + CANONICAL_URL + "\"}"))
                .andExpect(status().isCreated());

        assertThat(recipeRepository.count()).isEqualTo(2);
    }

    @Test
    void concurrentSameVideoImportsCreateOneRecipe() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        given(recipeTextAnalyzer.analyze(any())).willAnswer(invocation -> {
            barrier.await(5, TimeUnit.SECONDS);
            return validAnalysis();
        });
        RecipeSourceMetadata source = new RecipeSourceMetadata(
                CANONICAL_URL,
                VIDEO_ID,
                "https://i.ytimg.com/test.jpg"
        );

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<RecipeImportResult> first = executor.submit(() ->
                    recipeImportService.importUniqueSource(
                            user.getId(),
                            "첫 동시 자막",
                            RecipeImportType.URL,
                            source
                    ));
            Future<RecipeImportResult> second = executor.submit(() ->
                    recipeImportService.importUniqueSource(
                            user.getId(),
                            "둘째 동시 자막",
                            RecipeImportType.URL,
                            source
                    ));

            assertThat(List.of(
                    first.get(10, TimeUnit.SECONDS).created(),
                    second.get(10, TimeUnit.SECONDS).created()
            )).containsExactlyInAnyOrder(true, false);
        }

        assertThat(recipeRepository.count()).isEqualTo(1);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/recipes/import/youtube")
                        .contentType("application/json")
                        .content("{\"url\":\"" + CANONICAL_URL + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions performImport() throws Exception {
        return mockMvc.perform(post("/api/recipes/import/youtube")
                .header("Authorization", bearer())
                .contentType("application/json")
                .content("{\"url\":\"" + CANONICAL_URL + "\"}"));
    }

    private YouTubeVideoContent videoContent() {
        return new YouTubeVideoContent(
                VIDEO_ID,
                CANONICAL_URL,
                "계란 볶음밥 영상",
                "https://i.ytimg.com/test.jpg",
                "계란과 밥을 팬에서 볶습니다."
        );
    }

    private AnalyzedRecipe validAnalysis() {
        return new AnalyzedRecipe(
                "계란 볶음밥", "영상 레시피", 20,
                List.of(new AnalyzedRecipe.IngredientData(
                        "계란",
                        new BigDecimal("50"),
                        new NutritionSummary(
                                new BigDecimal("1.50"),
                                new BigDecimal("0.01"),
                                new BigDecimal("0.13"),
                                new BigDecimal("0.10"),
                                BigDecimal.ZERO,
                                new BigDecimal("0.01"),
                                new BigDecimal("1.24")
                        )
                )),
                List.of("팬에서 볶는다.")
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
