package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Provider 默认参数模板更新结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderParamUpdateResponse {

    private ContentType contentType;
    private String providerName;
    private String providerType;
    private String paramConfigSource;
    private Map<String, Object> defaultParams;
    private String message;
    private String updatedAt;
}
