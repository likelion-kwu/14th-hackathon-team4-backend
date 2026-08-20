package com.glucobite.auth.controller;

import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import com.glucobite.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUpBeforeTest() {
        cleanUp();
    }

    @AfterEach
    void cleanUpAfterTest() {
        cleanUp();
    }

    private void cleanUp() {
        refreshTokenRepository.deleteAll();
        healthProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void logsInWithValidCredentials() throws Exception {
        User user = userRepository.save(new User(
                "login-user",
                passwordEncoder.encode("1234"),
                "로그인 사용자"
        ));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("login-user", "1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        String subject = jwtDecoder.decode(response.get("accessToken").asString()).getSubject();
        assertThat(subject).isEqualTo(user.getId().toString());
    }

    @Test
    void rejectsWrongPasswordWithoutRevealingUserExistence() throws Exception {
        userRepository.save(new User(
                "login-user",
                passwordEncoder.encode("1234"),
                "로그인 사용자"
        ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("login-user", "9999")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void rejectsUnknownLoginIdWithSameResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unknown-user", "9999")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void rejectsBlankCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.loginId").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "12a4", "１２３４"})
    void rejectsPasswordsThatAreNotExactlyFourAsciiDigits(String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("login-user", password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    private String loginJson(String loginId, String password) {
        return """
                {
                  "loginId":"%s",
                  "password":"%s"
                }
                """.formatted(loginId, password);
    }
}
