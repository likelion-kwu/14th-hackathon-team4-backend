package com.glucobite.common.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

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
            allowedOrigins.forEach(CorsProperties::validateOrigin);
        }
    }

    /**
     * origin은 scheme, host, (선택적) port까지만 허용한다.
     *
     * <p>{@code setAllowedOrigins}는 이 세 요소만 비교하므로 path나 query가 붙은 값은
     * 어떤 요청과도 매칭되지 않는다. 그런 값을 그냥 통과시키면 CORS가 조용히 안 먹는 상태가
     * 되므로, 요청 처리 시점이 아니라 시작 시점에 거부한다.
     */
    private static void validateOrigin(String origin) {
        if (origin.contains("*")) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must not contain a wildcard: " + origin);
        }

        URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "CORS allowed origin is not a valid URI: " + origin, e);
        }

        String scheme = uri.getScheme() == null
                ? null
                : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must use the http or https scheme: " + origin);
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must include a host: " + origin);
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must not contain user info: " + origin);
        }
        if (uri.getPath() != null && !uri.getPath().isEmpty()) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must not contain a path: " + origin);
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must not contain a query string: " + origin);
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "CORS allowed origin must not contain a fragment: " + origin);
        }
    }
}
