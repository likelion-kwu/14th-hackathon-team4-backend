package com.glucobite.common.config;

import com.glucobite.common.security.JwtAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private static final String[] OPENAPI_PATHS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    private static final String[] PUBLIC_API_PATHS = {
            "/api/auth/**"
    };

    // CD의 배포 후 health check가 인증 없이 호출한다.
    private static final String[] HEALTH_PATHS = {
            "/actuator/health",
            "/actuator/health/**"
    };

    // Access Token 갱신에 HttpOnly Refresh Token cookie를 사용한다.
    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HEALTH_PATHS).permitAll()
                        .requestMatchers(OPENAPI_PATHS).permitAll()
                        .requestMatchers(PUBLIC_API_PATHS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/allergens").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint)
                )
                .build();
    }

    /**
     * 허용 origin은 환경변수로만 주입한다.
     *
     * <p>{@code setAllowedOrigins}는 scheme, host, port가 정확히 일치하는 값만 통과시킨다.
     * 패턴 매칭을 허용하는 {@code setAllowedOriginPatterns}는 사용하지 않는다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
