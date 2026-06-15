package com.anjing.aigc.model.request;

import com.anjing.aigc.model.enums.ContentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Provider 显式 smoke test 请求。
 */
@Data
@Schema(description = "Provider 显式 smoke test 请求")
public class ProviderSmokeTestRequest {

    @NotNull(message = "contentType 不能为空")
    @Schema(description = "内容类型；V1 仅支持 IMAGE", example = "IMAGE")
    private ContentType contentType;

    @NotBlank(message = "provider 不能为空")
    @Schema(description = "Provider 类型或名称", example = "GOOGLE")
    private String provider;

    @Schema(description = "Provider 展示名称", example = "Google Nano Banana")
    private String providerName;

    @Schema(description = "测试 Prompt；为空时使用最小验证 Prompt")
    private String prompt;

    @Schema(description = "确认会触发外部 Provider 调用；Google Provider 必须显式传 true", example = "true")
    private Boolean confirmExternalCall;
}
