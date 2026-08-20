package com.glucobite.recipe.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.recipe.dto.RecipeDetailResponse;
import com.glucobite.recipe.dto.RecipePageResponse;
import com.glucobite.recipe.dto.RecipeRecommendationResponse;
import com.glucobite.recipe.dto.RecipeStepListResponse;
import com.glucobite.recipe.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
@Tag(name = "Recipe", description = "사용자 레시피 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
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
            summary = "개인화 레시피 추천 조회",
            description = "소유 레시피 중 알레르기와 하루 탄수화물 목표를 만족하는 항목을 최신순으로 반환합니다."
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

    private Long parseUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
