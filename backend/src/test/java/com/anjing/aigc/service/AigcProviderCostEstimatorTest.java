package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.ProviderCostEstimate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AigcProviderCostEstimatorTest {

    private final AigcProperties properties = new AigcProperties();
    private final AigcProviderCostEstimator estimator = new AigcProviderCostEstimator(properties);

    @Test
    void estimateReturnsPendingBeforeTaskFinishes() {
        AigcTask task = task("Google Nano Banana", "GOOGLE", ContentType.IMAGE, null);

        ProviderCostEstimate estimate = estimator.estimate(task);

        assertEquals(AigcProviderCostEstimator.STATUS_PENDING, estimate.getCostStatus());
        assertNull(estimate.getEstimatedCostAmount());
    }

    @Test
    void estimateReturnsZeroForMockProvider() {
        AigcTask task = task("Mock Image Provider", "OTHER", ContentType.IMAGE, 1200L);

        ProviderCostEstimate estimate = estimator.estimate(task);

        assertEquals(AigcProviderCostEstimator.STATUS_MOCK_FREE, estimate.getCostStatus());
        assertEquals(0, estimate.getEstimatedCostAmount().compareTo(BigDecimal.ZERO));
        assertEquals("USD", estimate.getEstimatedCostCurrency());
    }

    @Test
    void estimateUsesConfiguredGoogleImageUnitCost() {
        properties.getCost().getGoogle().setImageUnitCost(new BigDecimal("0.040000"));
        AigcTask task = task("Google Nano Banana", "GOOGLE", ContentType.IMAGE, 2400L);

        ProviderCostEstimate estimate = estimator.estimate(task);

        assertEquals(AigcProviderCostEstimator.STATUS_ESTIMATED, estimate.getCostStatus());
        assertEquals(0, estimate.getEstimatedCostAmount().compareTo(new BigDecimal("0.040000")));
        assertEquals("task", estimate.getCostUnit());
    }

    @Test
    void estimateMarksGoogleCostAsNotConfiguredWhenUnitCostIsZero() {
        AigcTask task = task("Google Nano Banana", "GOOGLE", ContentType.IMAGE, 2400L);

        ProviderCostEstimate estimate = estimator.estimate(task);

        assertEquals(AigcProviderCostEstimator.STATUS_ESTIMATE_NOT_CONFIGURED, estimate.getCostStatus());
        assertNull(estimate.getEstimatedCostAmount());
    }

    private AigcTask task(String providerName, String providerType, ContentType contentType, Long durationMs) {
        AigcTask task = new AigcTask();
        task.setProviderName(providerName);
        task.setProviderType(providerType);
        task.setContentType(contentType);
        task.setDurationMs(durationMs);
        return task;
    }
}
