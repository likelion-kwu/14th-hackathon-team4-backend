package com.glucobite.auth.controller;

import com.glucobite.auth.entity.RefreshToken;
import com.glucobite.auth.repository.RefreshTokenRepository;
import com.glucobite.auth.service.RefreshTokenCodec;
import com.glucobite.health.repository.HealthProfileRepository;
import com.glucobite.user.entity.User;
import com.glucobite.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthRefreshIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HealthProfileRepository healthProfileRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private RefreshTokenCodec tokenCodec;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void loginIssuesSecureHttpOnlyCrossSiteCookieAndStoresOnlyHash() throws Exception {
        createUser("cookie-user");

        MvcResult result = login("cookie-user");
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String rawToken = cookieValue(setCookie);

        assertThat(setCookie)
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=None")
                .contains("Path=/api/auth");
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenCodec.hash(rawToken))
                .orElseThrow();
        assertThat(stored.getTokenHash()).isNotEqualTo(rawToken);
    }

    @Test
    void refreshRotatesTokenAndRejectsReuseByRevokingFamily() throws Exception {
        createUser("rotation-user");
        String original = cookieValue(login("rotation-user")
                .getResponse().getHeader(HttpHeaders.SET_COOKIE));

        MvcResult rotatedResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie(original)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        String replacement = cookieValue(
                rotatedResult.getResponse().getHeader(HttpHeaders.SET_COOKIE));

        assertThat(replacement).isNotEqualTo(original);
        assertThat(refreshTokenRepository.findByTokenHash(tokenCodec.hash(original))
                .orElseThrow().isRevoked()).isTrue();

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(original)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(replacement)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void concurrentRefreshAllowsOnlyOneRequest() throws Exception {
        createUser("concurrent-user");
        String original = cookieValue(login("concurrent-user")
                .getResponse().getHeader(HttpHeaders.SET_COOKIE));
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> refreshStatusAfter(start, original));
            Future<Integer> second = executor.submit(() -> refreshStatusAfter(start, original));
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder(200, 401);
        }
    }

    @Test
    void separateDeviceFamilyRemainsUsableAfterOtherFamilyReuse() throws Exception {
        createUser("multi-device-user");
        String firstDevice = cookieValue(login("multi-device-user")
                .getResponse().getHeader(HttpHeaders.SET_COOKIE));
        String secondDevice = cookieValue(login("multi-device-user")
                .getResponse().getHeader(HttpHeaders.SET_COOKIE));
        String firstReplacement = cookieValue(mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie(firstDevice)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE));

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(firstDevice)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(firstReplacement)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(secondDevice)))
                .andExpect(status().isOk());
    }

    @Test
    void expiredRandomAndMissingTokensShareUnauthorizedResponse() throws Exception {
        User user = createUser("expired-user");
        String expiredRaw = "expired-token";
        refreshTokenRepository.save(new RefreshToken(
                user,
                tokenCodec.hash(expiredRaw),
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)
        ));

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(expiredRaw)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie("random-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutIsIdempotentAndDeletesCookie() throws Exception {
        createUser("logout-user");
        String rawToken = cookieValue(login("logout-user")
                .getResponse().getHeader(HttpHeaders.SET_COOKIE));

        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie(rawToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("Max-Age=0")));
        mockMvc.perform(post("/api/auth/logout").cookie(refreshCookie(rawToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(rawToken)))
                .andExpect(status().isUnauthorized());
    }


    private User createUser(String loginId) {
        return userRepository.save(new User(
                loginId,
                passwordEncoder.encode("1234"),
                "인증 사용자"
        ));
    }

    private MvcResult login(String loginId) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"loginId":"%s","password":"1234"}
                                """.formatted(loginId)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Cookie refreshCookie(String value) {
        return new Cookie("refresh_token", value);
    }

    private int refreshStatusAfter(CountDownLatch start, String token) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie(token)))
                .andReturn().getResponse().getStatus();
    }

    private String cookieValue(String setCookie) {
        assertThat(setCookie).isNotBlank();
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private void cleanUp() {
        refreshTokenRepository.deleteAll();
        healthProfileRepository.deleteAll();
        userRepository.deleteAll();
    }
}
