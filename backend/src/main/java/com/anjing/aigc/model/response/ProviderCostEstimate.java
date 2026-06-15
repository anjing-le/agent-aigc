package com.anjing.aigc.model.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Provider 成本估算结果。
 */
@Data
@Builder
public class ProviderCostEstimate {

    private String costStatus;
    private BigDecimal estimatedCostAmount;
    private String estimatedCostCurrency;
    private String costUnit;
    private String costDescription;
}
