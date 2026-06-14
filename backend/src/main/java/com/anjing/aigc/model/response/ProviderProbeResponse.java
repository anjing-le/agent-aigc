package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Provider 运行前探测结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderProbeResponse {

    private ContentType contentType;
    private String requestedProvider;
    private String providerName;
    private String providerType;
    private String activeProvider;
    private String credentialSource;
    private Boolean registered;
    private Boolean active;
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
    private String checkedAt;
}
