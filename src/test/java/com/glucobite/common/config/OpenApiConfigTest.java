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
}
