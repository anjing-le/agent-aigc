package com.anjing.client;

import com.anjing.config.properties.RemoteHttpClientProperties;
import com.anjing.model.errorcode.RemoteErrorCode;
import com.anjing.model.exception.SystemException;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configuration-backed remote call policy.
 *
 * <p>It is disabled by default, but gives copied projects a concrete place to
 * start service-to-service governance without introducing a gateway, registry,
 * or circuit breaker dependency into the scaffold.</p>
 */
@RequiredArgsConstructor
public class ConfiguredRemoteCallPolicy implements RemoteCallPolicy {

    private static final String WILDCARD = "*";

    private final RemoteHttpClientProperties properties;

    @Override
    public void beforeCall(RemoteCallPolicyContext context) {
        RemoteHttpClientProperties.Policy policy = properties.getPolicy();
        if (policy == null || !policy.isEnabled()) {
            return;
        }

        if (matchesAny(policy.getBlockedServiceIds(), context.serviceId(), context.targetService())) {
            reject(context, "blocked service");
        }

        if (!isAllowed(policy.getAllowedCallerIds(), context.callerId())) {
            reject(context, "caller is not globally allowed");
        }

        List<String> serviceAllowedCallers = allowedCallersForService(
                policy.getAllowedCallerIdsByService(),
                context.serviceId(),
                context.targetService()
        );
        if (!isAllowed(serviceAllowedCallers, context.callerId())) {
            reject(context, "caller is not allowed for service");
        }
    }

    private List<String> allowedCallersForService(
            Map<String, List<String>> allowedCallerIdsByService,
            String serviceId,
            String targetService
    ) {
        if (allowedCallerIdsByService == null || allowedCallerIdsByService.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> byServiceId = allowedCallerIdsByService.get(normalize(serviceId));
        if (byServiceId != null) {
            return byServiceId;
        }
        return allowedCallerIdsByService.getOrDefault(normalize(targetService), Collections.emptyList());
    }

    private boolean isAllowed(List<String> allowedValues, String actual) {
        List<String> normalized = normalizedValues(allowedValues);
        return normalized.isEmpty()
                || normalized.contains(WILDCARD)
                || normalized.contains(normalize(actual));
    }

    private boolean matchesAny(List<String> expectedValues, String... actualValues) {
        List<String> normalizedExpected = normalizedValues(expectedValues);
        if (normalizedExpected.isEmpty()) {
            return false;
        }
        for (String actual : actualValues) {
            if (normalizedExpected.contains(normalize(actual))) {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizedValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private void reject(RemoteCallPolicyContext context, String reason) {
        throw new SystemException(
                String.format(
                        "远程调用策略拒绝: %s, service=%s, callerId=%s",
                        reason,
                        context.targetService(),
                        context.callerId()
                ),
                RemoteErrorCode.REMOTE_CALL_PERMISSION_DENIED
        );
    }
}
