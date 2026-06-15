package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Provider 运行时路由切换结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderRouteUpdateResponse {

    private ContentType contentType;
    private String activeProvider;
    private String routeConfigSource;
    private String providerName;
    private String providerType;
    private String credentialSource;
    private String credentialStorageMode;
    private Boolean available;
    private Boolean routable;
    private Boolean configurationComplete;
    private String configuredModel;
    private Map<String, Object> defaultParams;
    private String paramConfigSource;
    private String paramConfigUpdatedAt;
    private String missingConfig;
    private String statusReason;
    private String message;
    private String updatedAt;
}
