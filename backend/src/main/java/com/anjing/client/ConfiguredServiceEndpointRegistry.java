package com.anjing.client;

import com.anjing.config.properties.RemoteHttpClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * Configuration-backed service endpoint registry.
 */
@RequiredArgsConstructor
public class ConfiguredServiceEndpointRegistry implements ServiceEndpointRegistry {

    private static final String SOURCE = "configuration";

    private final RemoteHttpClientProperties properties;

    @Override
    public Optional<ServiceEndpoint> findEndpoint(String serviceId) {
        if (!StringUtils.hasText(serviceId)) {
            return Optional.empty();
        }

        Map<String, String> serviceBaseUrls = properties.getServiceBaseUrls();
        String baseUrl = serviceBaseUrls == null ? null : serviceBaseUrls.get(serviceId);
        if (!StringUtils.hasText(baseUrl)) {
            return Optional.empty();
        }

        return Optional.of(new ServiceEndpoint(serviceId, baseUrl, SOURCE));
    }
}
