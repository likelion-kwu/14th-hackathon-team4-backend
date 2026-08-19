package com.glucobite.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityTest {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issuesAndDecodesAccessToken() {
        IssuedToken issuedToken = jwtTokenService.issue(42L);

        Jwt jwt = jwtDecoder.decode(issuedToken.accessToken());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getIssuer().toString()).isEqualTo("https://api.glucobite.app");
        assertThat(issuedToken.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void rejectsProtectedRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/health/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void acceptsValidTokenBeforeControllerRouting() throws Exception {
        IssuedToken issuedToken = jwtTokenService.issue(42L);

        mockMvc.perform(get("/api/v1/health/profile")
                        .header("Authorization", "Bearer " + issuedToken.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/health/profile")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
