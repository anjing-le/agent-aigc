package com.anjing.config.properties;

import com.anjing.model.constants.ServiceBoundaryConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defaults for outbound HTTP calls to future internal services or third-party APIs.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.remote-http")
public class RemoteHttpClientProperties {

    /**
     * Default caller id propagated through X-Caller-Id.
     */
    private String defaultCallerId = ServiceBoundaryConstants.APPLICATION_ID;

    /**
     * Connection timeout for outbound HTTP calls.
     */
    private int connectTimeoutMs = 3000;

    /**
     * Read timeout for outbound HTTP calls.
     */
    private int readTimeoutMs = 5000;

    /**
     * Default retry count for calls using RemoteHttpClient.
     */
    private int defaultRetryCount = 0;

    /**
     * Default retry interval in milliseconds.
     */
    private long defaultRetryIntervalMs = 1000L;

    /**
     * Optional lightweight governance policy for outbound remote calls.
     */
    private Policy policy = new Policy();

    /**
     * Logical service id to base URL mapping.
     *
     * <p>Keep service addresses in configuration so business code can call
     * RemoteHttpRequest with serviceId + path instead of scattering absolute URLs.</p>
     */
    private Map<String, String> serviceBaseUrls = new LinkedHashMap<>();

    @Data
    public static class Policy {

        /**
         * Disabled by default so the scaffold stays dependency-free and permissive.
         */
        private boolean enabled = false;

        /**
         * Service ids or target service names that must be rejected before transport.
         */
        private List<String> blockedServiceIds = new ArrayList<>();

        /**
         * Global caller id allow list. Empty means all callers are allowed.
         */
        private List<String> allowedCallerIds = new ArrayList<>();

        /**
         * Per-service caller id allow list. Empty list means no service-level restriction.
         */
        private Map<String, List<String>> allowedCallerIdsByService = new LinkedHashMap<>();
    }
}
