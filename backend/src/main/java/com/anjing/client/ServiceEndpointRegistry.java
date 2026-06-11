package com.anjing.client;

import java.util.Optional;

/**
 * Lookup boundary for downstream service endpoints.
 *
 * <p>The scaffold default implementation reads static configuration. Future
 * projects can replace it with API Gateway, service discovery, region routing,
 * or gray release routing without changing HTTP call sites.</p>
 */
public interface ServiceEndpointRegistry {

    /**
     * Finds the base endpoint for a logical downstream service id.
     */
    Optional<ServiceEndpoint> findEndpoint(String serviceId);
}
