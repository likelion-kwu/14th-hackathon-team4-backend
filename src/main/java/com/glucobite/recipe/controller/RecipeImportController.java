package com.glucobite.recipe.controller;

import com.glucobite.common.config.OpenApiConfig;
import com.glucobite.recipe.dto.ImportedRecipeResponse;
import com.glucobite.recipe.dto.TextRecipeImportRequest;
import com.glucobite.recipe.service.RecipeImportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    public RecipeImportController(RecipeImportService recipeImportService) {
        this.recipeImportService = recipeImportService;
    }

    @PostMapping("/text")
    public ResponseEntity<ImportedRecipeResponse> importText(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TextRecipeImportRequest request
    ) {
        ImportedRecipeResponse response = recipeImportService.importText(
                Long.valueOf(jwt.getSubject()),
                request.text()
        );
        return ResponseEntity.created(URI.create("/api/recipes/" + response.recipeId()))
                .body(response);
    }
}
