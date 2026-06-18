package com.fallguys.itemservice.controller.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/items/health",
            "/actuator/health",
            "/items/swagger-ui/**",
            "/items/swagger-ui.html",
            "/items/v3/api-docs/**",
            "/internal/items/**"
    };

    private static final String[] WRITE_ROLES = {
            "ADMIN",
            "HQ_MANAGER",
            "HQ_STAFF"
    };

    private static final String[] USER_ROLES = {
            "ADMIN",
            "HQ_MANAGER",
            "HQ_STAFF",
            "BRANCH_MANAGER",
            "BRANCH_STAFF"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityProblemHandler securityProblemHandler,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/items", "/items").hasAnyRole(WRITE_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/items/batch").hasAnyRole(WRITE_ROLES)
                        .requestMatchers(HttpMethod.POST, "/items/batch").hasAnyRole(USER_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/items/code-check", "/items/code-check").hasAnyRole(WRITE_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/items/*", "/items/*").hasAnyRole(WRITE_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/items/*/activate", "/items/*/activate").hasAnyRole(WRITE_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/items/*/deactivate", "/items/*/deactivate").hasAnyRole(WRITE_ROLES)
                        .requestMatchers("/api/items/**", "/items/**").hasAnyRole(USER_ROLES)
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityProblemHandler)
                        .accessDeniedHandler(securityProblemHandler))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
