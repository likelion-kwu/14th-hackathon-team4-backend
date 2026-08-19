package com.glucobite.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CORS origin은 환경변수로만 주입되므로, 값이 잘못되면 요청 처리 시점이 아니라
 * 애플리케이션 시작 시점에 실패해야 한다.
 */
class CorsPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsCommaSeparatedOrigins() {
        contextRunner
                .withPropertyValues(
                        "app.cors.allowed-origins=https://a.example.com,https://b.example.com")
                .run(context -> assertThat(context.getBean(CorsProperties.class).allowedOrigins())
                        .containsExactly("https://a.example.com", "https://b.example.com"));
    }

    @Test
    void failsToStartWhenOriginsAreMissing() {
        contextRunner
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsToStartWhenOriginsAreEmpty() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsToStartOnWildcardOrigin() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=*")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsToStartOnSubdomainWildcardOrigin() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=https://*.example.com")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsToStartWhenSchemeIsMissing() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=example.com")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsToStartOnTrailingSlash() {
        contextRunner
                .withPropertyValues("app.cors.allowed-origins=https://example.com/")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CorsProperties.class)
    static class TestConfig {
    }
}
