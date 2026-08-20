package com.glucobite.recipe.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.common.exception.ApiErrorResponse;
import com.glucobite.recipe.dto.ImportedRecipeResponse;
import com.glucobite.recipe.dto.TextRecipeImportRequest;
import com.glucobite.recipe.dto.YouTubeRecipeImportRequest;
import com.glucobite.recipe.entity.RecipeImportType;
import com.glucobite.recipe.importing.RecipeSourceMetadata;
import com.glucobite.recipe.service.RecipeImportService;
import com.glucobite.recipe.youtube.YouTubeTranscriptProvider;
import com.glucobite.recipe.youtube.YouTubeUrlParser;
import com.glucobite.recipe.youtube.YouTubeVideoContent;
import com.glucobite.recipe.youtube.YouTubeVideoReference;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/recipes/import")
@Tag(name = "Recipe", description = "사용자 레시피 API")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH_SCHEME)
public class RecipeImportController {

    private final RecipeImportService recipeImportService;
    private final YouTubeUrlParser youTubeUrlParser;
    private final YouTubeTranscriptProvider youTubeTranscriptProvider;

    public RecipeImportController(
            RecipeImportService recipeImportService,
            YouTubeUrlParser youTubeUrlParser,
            YouTubeTranscriptProvider youTubeTranscriptProvider
    ) {
        this.recipeImportService = recipeImportService;
        this.youTubeUrlParser = youTubeUrlParser;
        this.youTubeTranscriptProvider = youTubeTranscriptProvider;
    }

    @PostMapping("/text")
    @Operation(
            summary = "텍스트 레시피 분석 및 저장",
            description = "붙여넣은 텍스트를 GPT로 분석해 completed=false인 BASE Recipe로 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "기본 Recipe 저장 성공",
                    content = @Content(schema = @Schema(implementation = ImportedRecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "빈 입력 또는 허용 길이 초과",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "레시피가 아니거나 분석 결과가 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "OpenAI 호출 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ImportedRecipeResponse> importText(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TextRecipeImportRequest request
    ) {
        ImportedRecipeResponse response = recipeImportService.importText(
                Long.valueOf(jwt.getSubject()),
                request.text()
        );
        return ResponseEntity.created(URI.create("/api/recipes/" + response.recipeId()))
                .body(response);
    }

    @PostMapping("/youtube")
    @Operation(
            summary = "YouTube 레시피 분석 및 저장",
            description = "YouTube 영상의 공개 자막을 분석해 completed=false인 BASE Recipe로 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "기본 Recipe 저장 성공",
                    content = @Content(schema = @Schema(implementation = ImportedRecipeResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못되었거나 지원하지 않는 YouTube URL",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "영상 또는 공개 자막을 사용할 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "502", description = "YouTube 또는 OpenAI 호출 실패",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ImportedRecipeResponse> importYouTube(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody YouTubeRecipeImportRequest request
    ) {
        YouTubeVideoReference reference = youTubeUrlParser.parse(request.url());
        YouTubeVideoContent content = youTubeTranscriptProvider.fetch(reference);
        ImportedRecipeResponse response = recipeImportService.importAnalyzedText(
                Long.valueOf(jwt.getSubject()),
                analysisText(content),
                RecipeImportType.URL,
                new RecipeSourceMetadata(
                        content.canonicalUrl(),
                        content.videoId(),
                        content.thumbnailUrl()
                )
        );
        return ResponseEntity.created(URI.create("/api/recipes/" + response.recipeId()))
                .body(response);
    }

    private String analysisText(YouTubeVideoContent content) {
        if (content.title() == null || content.title().isBlank()) {
            return content.transcript();
        }
        return "영상 제목: " + content.title() + "\n자막:\n" + content.transcript();
    }
}
