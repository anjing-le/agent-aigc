package com.anjing.client;

import com.anjing.model.errorcode.RemoteErrorCode;
import com.anjing.model.exception.SystemException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Registry-backed service endpoint resolver.
 */
@Component
@RequiredArgsConstructor
public class ConfiguredServiceEndpointResolver implements ServiceEndpointResolver {

    private final ServiceEndpointRegistry serviceEndpointRegistry;

    @Override
    public String resolveUrl(String serviceId, String path) {
        if (!StringUtils.hasText(serviceId)) {
            throw new SystemException("远程 HTTP serviceId 不能为空", RemoteErrorCode.REMOTE_CALL_PARAM_ERROR);
        }

        ServiceEndpoint endpoint = serviceEndpointRegistry.findEndpoint(serviceId).orElse(null);
        if (endpoint == null || !StringUtils.hasText(endpoint.baseUrl())) {
            throw new SystemException(
                    "远程 HTTP 服务未发现 endpoint: " + serviceId,
                    RemoteErrorCode.REMOTE_CALL_PARAM_ERROR
            );
        }

        return joinUrl(endpoint.baseUrl(), path);
    }

    private String joinUrl(String baseUrl, String path) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (!StringUtils.hasText(path)) {
            return normalizedBase;
        }

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
}
