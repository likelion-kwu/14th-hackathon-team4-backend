package com.glucobite.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void bindsApiKeyAndModel() {
        contextRunner
                .withPropertyValues(
                        "app.openai.api-key=test-key",
                        "app.openai.model=gpt-5.6-luna"
                )
                .run(context -> {
                    OpenAIProperties properties = context.getBean(OpenAIProperties.class);
                    assertThat(properties.apiKey()).isEqualTo("test-key");
                    assertThat(properties.model()).isEqualTo("gpt-5.6-luna");
                });
    }

    @Test
    void failsToStartWhenApiKeyIsMissing() {
        contextRunner
                .withPropertyValues("app.openai.model=gpt-5.6-luna")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsToStartWhenModelIsBlank() {
        contextRunner
                .withPropertyValues(
                        "app.openai.api-key=test-key",
                        "app.openai.model="
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void boundsOpenAiRequestToThirtySeconds() {
        assertThat(OpenAIConfig.REQUEST_TIMEOUT)
                .isLessThanOrEqualTo(Duration.ofSeconds(30));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OpenAIProperties.class)
    static class TestConfig {
    }
}
