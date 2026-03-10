package com.ghana.commoditymonitor.config;

import com.ghana.commoditymonitor.security.CurrentUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC configuration for custom argument resolvers and other web-related settings.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    /**
     * Adds custom argument resolvers to the Spring MVC framework.
     * <p>
     * Registers the {@link CurrentUserArgumentResolver} to enable
     * {@link com.ghana.commoditymonitor.security.CurrentUser} annotation
     * support in controller methods.
     * </p>
     * 
     * @param resolvers the list of configured resolvers to add to
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
