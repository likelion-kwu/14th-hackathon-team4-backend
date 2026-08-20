package com.glucobite.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesOpenApiDocumentWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Glucobite API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    void exposesSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void documentsSignupRequestAndResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.summary")
                        .value("통합 회원가입"))
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/signup'].post.responses['409']").exists())
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.loginId.example")
                        .value("glucobite01"))
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.password.writeOnly")
                        .value(true))
                .andExpect(jsonPath("$.components.schemas.SignupProfileRequest.properties.sex.enum",
                        containsInAnyOrder("MALE", "FEMALE")))
                .andExpect(jsonPath("$.components.schemas.SignupProfileRequest.properties.sex.enum",
                        not(hasItem("UNSPECIFIED"))));
    }

    @Test
    void documentsLoginAndCommonResponseSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.summary").value("로그인"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['401']").exists())
                .andExpect(jsonPath("$.components.schemas.TokenResponse.properties.accessToken").exists())
                .andExpect(jsonPath("$.components.schemas.TokenResponse.properties.tokenType.example")
                        .value("Bearer"))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.properties.fieldErrors")
                        .exists());
    }

    @Test
    void documentsPublicAllergenLookup() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/allergens'].get.summary")
                        .value("알레르기 목록 조회"))
                .andExpect(jsonPath("$.paths['/api/allergens'].get.parameters[0].name")
                        .value("query"))
                .andExpect(jsonPath("$.paths['/api/allergens'].get.parameters[0].required")
                        .value(false))
                .andExpect(jsonPath("$.paths['/api/allergens'].get.parameters[0].schema.maxLength")
                        .value(100))
                .andExpect(jsonPath("$.paths['/api/allergens'].get.responses['200'].content['*/*'].schema.items['$ref']")
                        .value("#/components/schemas/AllergenResponse"))
                .andExpect(jsonPath("$.paths['/api/allergens'].get.responses['400']").exists())
                .andExpect(jsonPath("$.components.schemas.AllergenResponse.properties.allergenId.example")
                        .value(2))
                .andExpect(jsonPath("$.components.schemas.AllergenResponse.properties.name.example")
                        .value("우유"));
    }

    @Test
    void documentsAuthenticatedRecipeList() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/recipes'].get.summary")
                        .value("내 레시피 목록 조회"))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[0].name")
                        .value("completed"))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[1].name")
                        .value("page"))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[1].schema.default")
                        .value(0))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[1].schema.minimum")
                        .value(0))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[2].name")
                        .value("size"))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[2].schema.default")
                        .value(20))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[2].schema.minimum")
                        .value(1))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.parameters[2].schema.maximum")
                        .value(100))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/RecipePageResponse"))
                .andExpect(jsonPath("$.paths['/api/recipes'].get.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/recipes'].get.responses['401']").exists())
                .andExpect(jsonPath("$.components.schemas.RecipePageResponse.properties.content.items['$ref']")
                        .value("#/components/schemas/RecipeSummaryResponse"))
                .andExpect(jsonPath("$.components.schemas.RecipeSummaryResponse.properties.importType.enum",
                        containsInAnyOrder("URL", "IMAGE", "TEXT")))
                .andExpect(jsonPath("$.components.schemas.RecipeSummaryResponse.properties.totalCalories")
                        .exists());
    }

    @Test
    void documentsGptRecipePersonalizationContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/recipes/{recipeId}/personalized'].get")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/recipes/{recipeId}/personalized'].post.summary")
                        .value("GPT 개인화 레시피 후보 생성"))
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/personalized'].post.security[0].bearerAuth"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/personalized'].post.requestBody.content['application/json'].schema['$ref']"
                ).value("#/components/schemas/GeneratePersonalizedRecipeRequest"))
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/personalized'].post.responses['201']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/personalized'].post.responses['400']"
                ).exists())
                .andExpect(jsonPath(
                        "$.paths['/api/recipes/{recipeId}/personalized'].post.responses['502']"
                ).exists())
                .andExpect(jsonPath(
                        "$.components.schemas.GeneratePersonalizedRecipeRequest.properties.previousCandidateId"
                ).exists())
                .andExpect(jsonPath(
                        "$.components.schemas.PersonalizedRecipeDetailResponse.properties.candidateRecipeId"
                ).exists())
                .andExpect(jsonPath(
                        "$.components.schemas.PersonalizedRecipeDetailResponse.properties.originalIngredients"
                ).exists())
                .andExpect(jsonPath("$.components.schemas.NutritionSummary.properties.sugar")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.NutritionSummary.properties.sodium")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.RecipeSummaryResponse.properties.recipeType.enum",
                        containsInAnyOrder("BASE", "PERSONALIZATION_CANDIDATE", "PERSONALIZED")));
    }

    @Test
    void documentsAuthenticatedHealthProfileReadAndUpdate() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/health/profile'].get.summary")
                        .value("내 건강 프로필 조회"))
                .andExpect(jsonPath("$.paths['/api/health/profile'].get.security[0].bearerAuth")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/health/profile'].get.responses['200'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/HealthProfileResponse"))
                .andExpect(jsonPath("$.paths['/api/health/profile'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/health/profile'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/health/profile'].put.summary")
                        .value("내 건강 프로필 수정"))
                .andExpect(jsonPath("$.paths['/api/health/profile'].put.requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/HealthProfileUpdateRequest"))
                .andExpect(jsonPath("$.paths['/api/health/profile'].put.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/health/profile'].put.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/health/profile'].put.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/health/profile'].put.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.HealthProfileUpdateRequest.properties.sex.enum",
                        containsInAnyOrder("MALE", "FEMALE")))
                .andExpect(jsonPath("$.components.schemas.HealthProfileUpdateRequest.properties.sex.enum",
                        not(hasItem("UNSPECIFIED"))))
                .andExpect(jsonPath("$.components.schemas.HealthProfileResponse.properties.allergens.items['$ref']")
                        .value("#/components/schemas/AllergenResponse"))
                .andExpect(jsonPath("$.components.schemas.HealthProfileResponse.properties.diabetesStatus")
                        .doesNotExist());
    }

    @Test
    void documentsTextRecipeImportContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.summary")
                        .value("텍스트 레시피 분석 및 저장"))
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.security[0].bearerAuth")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/TextRecipeImportRequest"))
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.responses['201'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ImportedRecipeResponse"))
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/text'].post.responses['502']").exists())
                .andExpect(jsonPath("$.components.schemas.TextRecipeImportRequest.properties.text.maxLength")
                        .value(50_000))
                .andExpect(jsonPath("$.components.schemas.ImportedRecipeResponse.properties.importType.enum",
                        containsInAnyOrder("URL", "IMAGE", "TEXT")))
                .andExpect(jsonPath("$.components.schemas.ImportedRecipeResponse.properties.recipeType.enum",
                        containsInAnyOrder("BASE", "PERSONALIZATION_CANDIDATE", "PERSONALIZED")))
                .andExpect(jsonPath("$.components.schemas.ImportedRecipeResponse.properties.completed")
                        .exists());
    }

    @Test
    void documentsYouTubeRecipeImportContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.summary")
                        .value("YouTube 레시피 분석 및 저장"))
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.security[0].bearerAuth")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.requestBody.content['application/json'].schema['$ref']")
                        .value("#/components/schemas/YouTubeRecipeImportRequest"))
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.responses['201'].content['*/*'].schema['$ref']")
                        .value("#/components/schemas/ImportedRecipeResponse"))
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/recipes/import/youtube'].post.responses['502']").exists())
                .andExpect(jsonPath("$.components.schemas.YouTubeRecipeImportRequest.properties.url.maxLength")
                        .value(500))
                .andExpect(jsonPath("$.components.schemas.ImportedRecipeResponse.properties.sourceUrl")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ImportedRecipeResponse.properties.sourceExternalId")
                        .exists())
                .andExpect(jsonPath("$.components.schemas.ImportedRecipeResponse.properties.imageUrl")
                        .exists());
    }
}
