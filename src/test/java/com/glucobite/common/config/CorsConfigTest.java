package com.glucobite.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 허용 origin은 테스트 설정(src/test/resources/application.yaml)의
 * {@code app.cors.allowed-origins} 값을 따른다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN = "https://allowed.example.com";
    private static final String UNKNOWN_ORIGIN = "https://evil.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsPreflightFromAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void rejectsPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", UNKNOWN_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void rejectsPreflightFromSameHostWithDifferentScheme() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://allowed.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void rejectsPreflightFromSameHostWithDifferentPort() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void allowsAuthorizationAndContentTypeHeadersInPreflight() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Access-Control-Allow-Headers",
                        "Authorization, Content-Type"));
    }

    @Test
    void rejectsPreflightForHeaderOutsideAllowList() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-Custom-Header"))
                .andExpect(status().isForbidden());
    }

    @Test
    void doesNotAllowCredentials() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    void addsCorsHeaderToActualRequestFromAllowedOrigin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", ALLOWED_ORIGIN)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }
}
