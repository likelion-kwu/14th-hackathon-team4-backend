package com.glucobite.recipe.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.recipe.dto.GenerateIngredientAlternativesRequest;
import com.glucobite.recipe.dto.IngredientAlternativeListResponse;
import com.glucobite.recipe.dto.IngredientAlternativeSuggestionListResponse;
import com.glucobite.recipe.dto.GeneratePersonalizedRecipeRequest;
import com.glucobite.recipe.dto.PersonalizedRecipeDetailResponse;
import com.glucobite.recipe.dto.RecipeDetailResponse;
import com.glucobite.recipe.dto.RecipePageResponse;
import com.glucobite.recipe.dto.RecipeRecommendationResponse;
import com.glucobite.recipe.dto.RecipeStepListResponse;
import com.glucobite.recipe.dto.RecipeSubstitutionPreviewResponse;
import com.glucobite.recipe.dto.RecipeSubstitutionRequest;
import com.glucobite.recipe.dto.SavePersonalizedRecipeRequest;
import com.glucobite.recipe.dto.SavedPersonalizedRecipeResponse;
import com.glucobite.recipe.service.RecipeService;
import com.glucobite.recipe.service.RecipePersonalizationCandidateService;
import com.glucobite.recipe.service.RecipePersonalizationService;
import com.glucobite.recipe.service.RecipeSubstitutionSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipe", description = "사용자 레시피 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipePersonalizationService personalizationService;
    private final RecipePersonalizationCandidateService candidateService;
    private final RecipeSubstitutionSuggestionService substitutionSuggestionService;

    public RecipeController(
            RecipeService recipeService,
            RecipePersonalizationService personalizationService,
            RecipePersonalizationCandidateService candidateService,
            RecipeSubstitutionSuggestionService substitutionSuggestionService
    ) {
        this.recipeService = recipeService;
        this.personalizationService = personalizationService;
        this.candidateService = candidateService;
        this.substitutionSuggestionService = substitutionSuggestionService;
    }

    @GetMapping
    @Operation(
            summary = "내 레시피 목록 조회",
            description = "로그인 사용자가 저장한 레시피를 완료 상태로 필터링해 페이지 단위로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecipePageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "필터 또는 페이지 요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 토큰 누락 또는 오류",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    public RecipePageResponse getRecipes(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt,

            @Parameter(description = "완료 상태 필터. 미입력 시 전체 레시피를 반환합니다.")
            @RequestParam(required = false) Boolean completed,

            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Long userId = Long.valueOf(jwt.getSubject());
        return recipeService.findRecipes(userId, completed, page, size);
    }

    @GetMapping("/{recipeId}")
    @Operation(
            summary = "레시피 상세 조회",
            description = "로그인 사용자가 소유한 레시피의 기본 정보, 재료, 조리 단계와 영양 정보를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecipeDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 레시피가 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public RecipeDetailResponse getRecipe(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long recipeId
    ) {
        return recipeService.getRecipeDetail(parseUserId(jwt), recipeId);
    }

    @GetMapping("/recommendations")
    @Operation(
            summary = "보유 레시피 건강 조건 필터링",
            description = "이미 보유한 BASE/PERSONALIZED Recipe 중 알레르기와 하루 탄수화물 목표를 "
                    + "만족하는 항목을 최신순으로 반환합니다. GPT 개인화 후보 생성이나 재추천 API가 아닙니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecipeRecommendationResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "건강 프로필 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public RecipeRecommendationResponse getRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return recipeService.getRecommendations(parseUserId(jwt));
    }

    @GetMapping("/{recipeId}/steps")
    @Operation(summary = "조리 단계 조회", description = "step_order 오름차순으로 조리 단계를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = RecipeStepListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 레시피가 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public RecipeStepListResponse getRecipeSteps(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long recipeId
    ) {
        return recipeService.getRecipeSteps(parseUserId(jwt), recipeId);
    }

    @PostMapping("/{recipeId}/personalized")
    @Operation(summary = "GPT 개인화 레시피 후보 생성",
            description = "completed=false인 BASE Recipe와 건강 프로필을 분석해 GPT 후보를 생성하고 저장합니다. "
                    + "다른 후보를 원하면 직전 candidate ID를 전달합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "후보 생성 성공",
                    content = @Content(schema = @Schema(implementation = PersonalizedRecipeDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "개인화할 수 없는 Recipe 단계",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 레시피 또는 건강 프로필이 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "OpenAI 호출 또는 응답 검증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PersonalizedRecipeDetailResponse> generatePersonalizedRecipe(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long recipeId,
            @RequestBody(required = false) GeneratePersonalizedRecipeRequest request
    ) {
        PersonalizedRecipeDetailResponse response = candidateService.generate(
                parseUserId(jwt), recipeId, request
        );
        return ResponseEntity.created(URI.create("/api/recipes/" + response.candidateRecipeId()))
                .body(response);
    }

    @GetMapping("/{recipeId}/ingredients/{ingredientId}/alternatives")
    @Operation(summary = "대체 가능 재료 조회",
            description = "사용자 건강 프로필에 안전한 등록 대체 재료만 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = IngredientAlternativeListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 레시피, 재료 또는 건강 프로필이 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public IngredientAlternativeListResponse getAlternatives(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long recipeId,
            @PathVariable Long ingredientId
    ) {
        return personalizationService.getAlternatives(parseUserId(jwt), recipeId, ingredientId);
    }

    @PostMapping("/{recipeId}/ingredients/{ingredientId}/alternatives")
    @Operation(summary = "사용자 입력 기반 대체 재료 후보 생성",
            description = "등록 대체재를 먼저 확인하고, 필요하면 OpenAI web search로 Recipe 범위 후보를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 대체재, 캐시 또는 새 AI 후보 반환",
                    content = @Content(schema = @Schema(implementation = IngredientAlternativeSuggestionListResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 또는 제외 후보가 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 레시피, 재료 또는 건강 프로필이 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "OpenAI 호출 또는 응답 검증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public IngredientAlternativeSuggestionListResponse generateAlternatives(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long recipeId,
            @PathVariable Long ingredientId,
            @Valid @RequestBody GenerateIngredientAlternativesRequest request
    ) {
        return substitutionSuggestionService.generate(
                parseUserId(jwt),
                recipeId,
                ingredientId,
                request
        );
    }

    @PostMapping("/{recipeId}/substitutions/preview")
    @Operation(summary = "복수 대체 재료 미리보기",
            description = "현재까지 선택한 전체 대체 항목을 적용해 누적 결과를 계산합니다. 원본은 변경하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "미리보기 성공",
                    content = @Content(schema = @Schema(implementation = RecipeSubstitutionPreviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "중복, 충돌 또는 안전하지 않은 대체 재료",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 레시피, 재료 또는 건강 프로필이 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public RecipeSubstitutionPreviewResponse previewSubstitutions(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long recipeId,
            @Valid @RequestBody RecipeSubstitutionRequest request
    ) {
        return personalizationService.previewSubstitutions(parseUserId(jwt), recipeId, request);
    }

    @PostMapping("/{recipeId}/substitutions")
    @Operation(summary = "개인화 레시피 최종 저장",
            description = "전체 대체 항목을 반영한 새 레시피를 저장합니다. 원본 레시피는 변경하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "저장 성공",
                    content = @Content(schema = @Schema(implementation = SavedPersonalizedRecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "중복, 충돌 또는 안전하지 않은 대체 재료",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "소유한 레시피 또는 건강 프로필이 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SavedPersonalizedRecipeResponse> savePersonalizedRecipe(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long recipeId,
            @Valid @RequestBody SavePersonalizedRecipeRequest request
    ) {
        SavedPersonalizedRecipeResponse response = personalizationService.savePersonalizedRecipe(
                parseUserId(jwt),
                recipeId,
                request
        );
        return ResponseEntity.created(URI.create("/api/recipes/" + response.recipeId()))
                .body(response);
    }

    private Long parseUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
