package com.anjing.aigc.model.response;

import com.anjing.aigc.model.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Provider 凭证更新结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderCredentialUpdateResponse {

    private ContentType contentType;
    private String providerName;
    private String providerType;
    private String credentialSource;
    private Boolean configurationComplete;
    private Boolean available;
    private String statusReason;
    private String message;
    private String updatedAt;
}
