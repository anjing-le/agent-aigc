package com.anjing.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Request contract for outbound HTTP calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoteHttpRequest {

    @Builder.Default
    private HttpMethod method = HttpMethod.GET;

    /**
     * Absolute URL for the downstream service.
     *
     * <p>Prefer serviceId + path for internal services. Keep url for third-party APIs
     * or compatibility with legacy call sites.</p>
     */
    private String url;

    /**
     * Logical service id resolved by app.remote-http.service-base-urls.
     */
    private String serviceId;

    /**
     * Service-relative path, for example /api/auth/me.
     */
    private String path;

    /**
     * Logical service name used for logs and audit.
     */
    private String targetService;

    /**
     * Optional caller id. Defaults to app.remote-http.default-caller-id.
     */
    private String callerId;

    /**
     * Optional request body.
     */
    private Object body;

    /**
     * Extra headers. Do not put secrets here unless the log descriptor remains sanitized.
     */
    @Builder.Default
    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * Optional retry override.
     */
    private Integer retryCount;

    /**
     * Optional retry interval override.
     */
    private Long retryIntervalMs;

    /**
     * Whether RemoteCallWrapper should validate APIResponse and legacy BaseResponse success fields.
     */
    @Builder.Default
    private boolean checkResponse = true;
}
