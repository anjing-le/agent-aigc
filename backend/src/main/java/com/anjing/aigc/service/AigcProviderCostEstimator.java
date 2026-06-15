package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcTask;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.model.response.ProviderCostEstimate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Provider 调用成本估算。
 *
 * <p>V1 只做可配置估算，不内置官方价格；后续可替换为真实用量计费。</p>
 */
@Service
@RequiredArgsConstructor
public class AigcProviderCostEstimator {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_MOCK_FREE = "MOCK_FREE";
    public static final String STATUS_ESTIMATED = "ESTIMATED";
    public static final String STATUS_ESTIMATE_NOT_CONFIGURED = "ESTIMATE_NOT_CONFIGURED";
    public static final String STATUS_UNTRACKED = "UNTRACKED";

    private static final String UNIT_TASK = "task";

    private final AigcProperties aigcProperties;

    public ProviderCostEstimate estimate(AigcTask task) {
        String currency = normalizeCurrency(aigcProperties.getCost().getCurrency());
        if (task.getDurationMs() == null) {
            return estimate(STATUS_PENDING, null, currency, UNIT_TASK, "任务仍在执行，成本统计等待完成");
        }
        if (isMockProvider(task)) {
            return estimate(STATUS_MOCK_FREE, BigDecimal.ZERO, currency, UNIT_TASK, "本地 mock provider 不产生外部模型成本");
        }
        if ("GOOGLE".equals(task.getProviderType())) {
            BigDecimal unitCost = resolveGoogleUnitCost(task.getContentType());
            if (unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0) {
                return estimate(STATUS_ESTIMATED, unitCost, currency, UNIT_TASK, "按本地配置的 Google 单任务估算单价计算");
            }
            return estimate(STATUS_ESTIMATE_NOT_CONFIGURED, null, currency, UNIT_TASK, "Google 成本单价未配置");
        }
        return estimate(STATUS_UNTRACKED, null, currency, UNIT_TASK, "该 Provider 暂未接入成本估算");
    }

    private ProviderCostEstimate estimate(String status, BigDecimal amount, String currency, String unit, String description) {
        return ProviderCostEstimate.builder()
                .costStatus(status)
                .estimatedCostAmount(amount)
                .estimatedCostCurrency(currency)
                .costUnit(unit)
                .costDescription(description)
                .build();
    }

    private boolean isMockProvider(AigcTask task) {
        return "OTHER".equals(task.getProviderType())
                && task.getProviderName() != null
                && task.getProviderName().toLowerCase(Locale.ROOT).contains("mock");
    }

    private BigDecimal resolveGoogleUnitCost(ContentType contentType) {
        if (contentType == null) {
            return null;
        }
        AigcProperties.GoogleCostConfig google = aigcProperties.getCost().getGoogle();
        return switch (contentType) {
            case IMAGE -> google.getImageUnitCost();
            case VIDEO -> google.getVideoUnitCost();
            case AUDIO -> google.getAudioUnitCost();
            case TEXT -> BigDecimal.ZERO;
        };
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "USD" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
