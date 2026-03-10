package com.ghana.commoditymonitor.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;

@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("dashboardSummary")
        ));
        return cacheManager;
    }

    @Scheduled(fixedRate = 300000)
    public void evictDashboardCache() {
        CacheManager cacheManager = cacheManager();
        if (cacheManager.getCache("dashboardSummary") != null) {
            cacheManager.getCache("dashboardSummary").clear();
        }
    }
}
