package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider 显式 smoke test 响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderSmokeTestResponse {

    private ContentType contentType;
    private String providerName;
    private String providerType;
    private String taskId;
    private String assetId;
    private Boolean success;
    private String status;
    private String model;
    private String prompt;
    private String url;
    private String thumbnailUrl;
    private Long durationMs;
    private ProviderExecutionSummary providerExecution;
    private String errorCode;
    private String errorMessage;
    private String message;
    private String checkedAt;
}
