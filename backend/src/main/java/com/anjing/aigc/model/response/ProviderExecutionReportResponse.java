package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AIGC Provider execution report")
public class ProviderExecutionReportResponse {

    @Schema(description = "Report time window in days")
    private Integer days;

    @Schema(description = "Optional content type filter")
    private ContentType contentType;

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

    @Schema(description = "Provider grouped metrics")
    private List<ProviderExecutionMetricResponse> providerMetrics;

    @Schema(description = "Model grouped metrics")
    private List<ProviderExecutionMetricResponse> modelMetrics;

    @Schema(description = "Content type grouped metrics")
    private List<ProviderExecutionMetricResponse> contentTypeMetrics;

    @Schema(description = "Report generation time")
    private String generatedAt;
}
