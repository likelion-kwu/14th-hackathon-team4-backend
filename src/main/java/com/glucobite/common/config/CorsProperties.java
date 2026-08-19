package com.glucobite.common.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 브라우저에서 API를 호출할 수 있는 프론트엔드 origin 목록.
 *
 * <p>값은 코드에 하드코딩하지 않고 {@code CORS_ALLOWED_ORIGINS} 환경변수로 주입한다.
 * 환경변수가 없거나 비어 있으면 애플리케이션이 시작되지 않는다.
 */
@Validated
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {

    public CorsProperties {
        if (allowedOrigins != null) {
            allowedOrigins = List.copyOf(allowedOrigins);
            for (String origin : allowedOrigins) {
                if (origin.contains("*")) {
                    throw new IllegalArgumentException(
                            "CORS allowed origin must not contain a wildcard: " + origin);
                }
                if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
                    throw new IllegalArgumentException(
                            "CORS allowed origin must include a scheme: " + origin);
                }
                if (origin.endsWith("/")) {
                    throw new IllegalArgumentException(
                            "CORS allowed origin must not end with a slash: " + origin);
                }
            }
        }
    }
}
