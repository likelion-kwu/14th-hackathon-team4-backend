package com.glucobite.auth.service;

import com.glucobite.auth.config.RefreshTokenProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.Cookie;

import java.time.Duration;

@Component
public class RefreshTokenCookieService {

    private static final String COOKIE_PATH = "/api/auth";

    private final RefreshTokenProperties properties;

    public RefreshTokenCookieService(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String rawToken) {
        return baseCookie(rawToken)
                .maxAge(properties.expiration())
                .build();
    }

    public ResponseCookie delete() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String read(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.cookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite(properties.cookieSameSite())
                .path(COOKIE_PATH);
        if (properties.cookieDomain() != null) {
            builder.domain(properties.cookieDomain());
        }
        return builder;
    }
}
