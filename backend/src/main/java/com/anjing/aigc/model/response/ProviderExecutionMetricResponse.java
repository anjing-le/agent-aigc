package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AIGC Provider execution metric")
public class ProviderExecutionMetricResponse {

    @Schema(description = "Metric dimension, such as provider, model, or contentType")
    private String dimension;

    @Schema(description = "Display label")
    private String label;

    @Schema(description = "Content type when the metric belongs to one type")
    private ContentType contentType;

    @Schema(description = "Provider display name")
    private String providerName;

    @Schema(description = "Provider type")
    private String providerType;

    @Schema(description = "Model name")
    private String model;

    @Schema(description = "Total task count")
    private Long totalTasks;

    @Schema(description = "Completed task count")
    private Long completedTasks;

    @Schema(description = "Failed task count")
    private Long failedTasks;

    @Schema(description = "Pending or running task count")
    private Long pendingTasks;

    @Schema(description = "Success rate percentage, 0-100")
    private Double successRate;

    @Schema(description = "Average task duration in milliseconds")
    private Long averageDurationMs;

    @Schema(description = "Estimated total cost amount")
    private BigDecimal estimatedCostAmount;

    @Schema(description = "Estimated cost currency")
    private String estimatedCostCurrency;

    @Schema(description = "Cost status summary")
    private String costStatusSummary;
}
