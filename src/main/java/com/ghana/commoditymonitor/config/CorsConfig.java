package com.ghana.commoditymonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for Cross-Origin Resource Sharing (CORS).
 * Allowed origins are externalized via the {@code app.cors.allowed-origins} property,
 * which defaults to {@code http://localhost:5173} for local development.
 * In production, inject {@code CORS_ALLOWED_ORIGINS} via SSM / environment variable
 * (e.g. {@code https://<cloudfront-domain>}).
 * Multiple origins can be specified as a comma-separated list.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        boolean isWildcard = "*".equals(allowedOrigins);
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(!isWildcard) // Must be false when origins is "*"
                .maxAge(3600);
    }
}
