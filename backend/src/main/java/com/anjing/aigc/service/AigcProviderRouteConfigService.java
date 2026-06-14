package com.anjing.aigc.service;

import com.anjing.aigc.config.AigcProperties;
import com.anjing.aigc.model.entity.AigcProviderRouteConfig;
import com.anjing.aigc.model.enums.ContentType;
import com.anjing.aigc.provider.ContentProvider;
import com.anjing.aigc.repository.AigcProviderRouteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provider 路由配置来源。
 *
 * <p>优先读取数据库中的页面配置；没有页面配置时回落到环境变量和 yml。</p>
 */
@Service
@RequiredArgsConstructor
public class AigcProviderRouteConfigService {

    private static final String UPDATED_BY_RUNTIME_PAGE = "runtime-page";

    private final AigcProperties aigcProperties;
    private final AigcProviderRouteConfigRepository routeConfigRepository;

    public String getActiveProvider(ContentType contentType) {
        return routeConfigRepository.findByContentType(contentType)
                .map(AigcProviderRouteConfig::getActiveProvider)
                .filter(value -> value != null && !value.isBlank())
                .orElseGet(() -> getConfiguredActiveProvider(contentType));
    }

    public String getConfiguredActiveProvider(ContentType contentType) {
        return switch (contentType) {
            case IMAGE -> aigcProperties.getImage().getActiveProvider();
            case VIDEO -> aigcProperties.getVideo().getActiveProvider();
            case AUDIO -> aigcProperties.getAudio().getActiveProvider();
            case TEXT -> "";
        };
    }

    public String getRouteConfigSource(ContentType contentType) {
        return routeConfigRepository.findByContentType(contentType)
                .map(config -> "database")
                .orElse("configuration");
    }

    @Transactional
    public AigcProviderRouteConfig saveActiveProvider(ContentType contentType, String activeProvider,
            ContentProvider provider) {
        AigcProviderRouteConfig config = routeConfigRepository.findByContentType(contentType)
                .orElseGet(AigcProviderRouteConfig::new);
        config.setContentType(contentType);
        config.setActiveProvider(activeProvider);
        config.setProviderName(provider.getProviderName());
        config.setProviderType(provider.getProviderType().name());
        config.setUpdatedBy(UPDATED_BY_RUNTIME_PAGE);
        return routeConfigRepository.save(config);
    }
}
