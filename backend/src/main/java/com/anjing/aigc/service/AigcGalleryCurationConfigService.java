package com.anjing.aigc.service;

import com.anjing.aigc.model.entity.AigcGalleryCurationConfig;
import com.anjing.aigc.model.response.GalleryCurationRuleResponse;
import com.anjing.aigc.repository.AigcGalleryCurationConfigRepository;
import com.anjing.context.GlobalRequestContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 灵感广场运营规则配置来源。
 *
 * <p>V1 先把运营开关、数量和运营建议持久化；排序权重和专题算法后续再接入规则引擎。</p>
 */
@Service
@RequiredArgsConstructor
public class AigcGalleryCurationConfigService {

    public static final String CONFIG_SOURCE_BUILT_IN = "built-in";
    public static final String CONFIG_SOURCE_DATABASE = "database";

    private static final String UPDATED_BY_RUNTIME_PAGE = "runtime-page";

    private final AigcGalleryCurationConfigRepository configRepository;

    public List<GalleryCurationRuleResponse> applyConfigs(List<GalleryCurationRuleResponse> baseRules) {
        if (baseRules == null || baseRules.isEmpty()) {
            return List.of();
        }
        Map<String, AigcGalleryCurationConfig> configs = configRepository
                .findByRuleIdIn(baseRules.stream().map(GalleryCurationRuleResponse::getId).toList())
                .stream()
                .collect(Collectors.toMap(AigcGalleryCurationConfig::getRuleId, Function.identity()));

        baseRules.forEach(rule -> applyConfig(rule, configs.get(rule.getId())));
        return baseRules;
    }

    @Transactional
    public AigcGalleryCurationConfig saveConfig(String ruleId, Boolean enabled, Integer defaultSize, Integer maxSize,
            String operationHint) {
        AigcGalleryCurationConfig config = configRepository.findByRuleId(ruleId)
                .orElseGet(AigcGalleryCurationConfig::new);
        config.setRuleId(ruleId);
        config.setEnabled(enabled);
        config.setDefaultSize(defaultSize);
        config.setMaxSize(maxSize);
        config.setOperationHint(normalizeHint(operationHint));
        config.setUpdatedBy(resolveUpdatedBy());
        return configRepository.save(config);
    }

    private void applyConfig(GalleryCurationRuleResponse rule, AigcGalleryCurationConfig config) {
        rule.setConfigSource(CONFIG_SOURCE_BUILT_IN);
        if (config == null) {
            return;
        }
        if (config.getEnabled() != null) {
            rule.setEnabled(config.getEnabled());
        }
        if (config.getDefaultSize() != null) {
            rule.setDefaultSize(config.getDefaultSize());
        }
        if (config.getMaxSize() != null) {
            rule.setMaxSize(config.getMaxSize());
        }
        if (StringUtils.hasText(config.getOperationHint())) {
            rule.setOperationHint(config.getOperationHint());
        }
        rule.setConfigSource(CONFIG_SOURCE_DATABASE);
        rule.setUpdatedBy(config.getUpdatedBy());
        rule.setUpdatedAt(config.getUpdatedAt() == null ? null : config.getUpdatedAt().toString());
    }

    private String normalizeHint(String operationHint) {
        if (!StringUtils.hasText(operationHint)) {
            return null;
        }
        return operationHint.trim();
    }

    private String resolveUpdatedBy() {
        return GlobalRequestContextHolder.current()
                .map(context -> StringUtils.hasText(context.getUserName()) ? context.getUserName() : context.getUserId())
                .filter(StringUtils::hasText)
                .orElse(UPDATED_BY_RUNTIME_PAGE);
    }
}
