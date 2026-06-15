package com.anjing.aigc.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Provider 调用观测摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderExecutionSummary {

    /** 实际调用的 Provider 名称 */
    private String providerName;

    /** Provider 类型 */
    private String providerType;

    /** 实际使用的模型 */
    private String model;

    /** 任务耗时（毫秒） */
    private Long durationMs;

    /** 成本统计状态：PENDING、MOCK_FREE、ESTIMATED、ESTIMATE_NOT_CONFIGURED、UNTRACKED */
    private String costStatus;

    /** 估算成本金额 */
    private BigDecimal estimatedCostAmount;

    /** 估算成本币种 */
    private String estimatedCostCurrency;

    /** 成本估算单位 */
    private String costUnit;

    /** 成本估算说明 */
    private String costDescription;
}
