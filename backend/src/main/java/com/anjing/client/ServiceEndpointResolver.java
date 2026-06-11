package com.anjing.client;

/**
 * Resolves logical service ids to callable HTTP URLs.
 *
 * <p>The default scaffold implementation reads static base URLs from
 * configuration. Future projects can replace this interface with API Gateway,
 * service discovery, region-aware routing, or gray release routing without
 * changing call sites.</p>
 */
public interface ServiceEndpointResolver {

    /**
     * Resolves a service-relative path to an absolute URL.
     *
     * @param serviceId logical downstream service id
     * @param path      service-relative API path
     * @return absolute URL
     */
    String resolveUrl(String serviceId, String path);
}
