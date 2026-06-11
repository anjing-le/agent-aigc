package com.anjing.client;

/**
 * Resolves the outbound service caller identity.
 *
 * <p>Downstream services can replace this when caller identity comes from an
 * auth center, API gateway, service mesh, or service registry policy.</p>
 */
public interface RemoteCallerResolver {

    /**
     * Resolve the caller id propagated through X-Caller-Id.
     */
    String resolveCallerId(RemoteHttpRequest request);
}
