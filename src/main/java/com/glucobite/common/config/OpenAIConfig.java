package com.glucobite.common.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OpenAIProperties.class)
public class OpenAIConfig {

    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    public OpenAIClient openAIClient(OpenAIProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .timeout(REQUEST_TIMEOUT)
                .maxRetries(0)
                .build();
    }

}
