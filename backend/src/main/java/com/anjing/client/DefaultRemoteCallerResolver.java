package com.anjing.client;

import com.anjing.config.properties.RemoteHttpClientProperties;
import com.anjing.model.constants.ServiceBoundaryConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * Default caller resolver based on explicit request override and application configuration.
 */
@RequiredArgsConstructor
public class DefaultRemoteCallerResolver implements RemoteCallerResolver {

    private final RemoteHttpClientProperties properties;

    @Override
    public String resolveCallerId(RemoteHttpRequest request) {
        if (request != null && StringUtils.hasText(request.getCallerId())) {
            return request.getCallerId();
        }
        if (StringUtils.hasText(properties.getDefaultCallerId())) {
            return properties.getDefaultCallerId();
        }
        return ServiceBoundaryConstants.APPLICATION_ID;
    }
}
