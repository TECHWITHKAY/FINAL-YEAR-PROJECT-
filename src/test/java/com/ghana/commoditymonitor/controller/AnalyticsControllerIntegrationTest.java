package com.ghana.commoditymonitor.controller;

import com.ghana.commoditymonitor.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the AnalyticsController.
 * Tests analytics endpoints against a real PostgreSQL container with seeded data.
 */
class AnalyticsControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getVolatility_ShouldReturn200() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/analytics/volatility", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"success\":true");
    }

    @Test
    void getMonthlyTrend_WithInvalidCommodityId_ShouldReturn404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/analytics/trends/999999?months=6", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCityComparison_WithValidCommodityId_ShouldReturn200() {
        // Commodity ID 1 is 'Maize' from seed data
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/analytics/city-comparison/1", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"success\":true");
    }

    @Test
    void getInflationTrend_WithValidCommodityId_ShouldReturnOk() {
        // Commodity ID 1 is 'Maize' — may return 200 or 204 depending on data presence
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/analytics/inflation/1", String.class);
        assertThat(response.getStatusCode().value()).isIn(200, 204);
    }

    @Test
    void getForecast_WithValidCommodityId_ShouldReturnOk() {
        // Commodity ID 1 is 'Maize' — may return 200 or 204 depending on data presence
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/analytics/forecast/1", String.class);
        assertThat(response.getStatusCode().value()).isIn(200, 204);
    }
}
