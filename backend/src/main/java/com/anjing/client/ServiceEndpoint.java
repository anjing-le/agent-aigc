package com.anjing.client;

/**
 * Resolved downstream service endpoint.
 *
 * <p>The source field helps downstream adapters distinguish configuration,
 * gateway, service discovery, region routing, or gray release decisions.</p>
 */
public record ServiceEndpoint(
        String serviceId,
        String baseUrl,
        String source
) {
}
