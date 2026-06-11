package com.anjing.client;

/**
 * Sanitized remote call facts passed to {@link RemoteCallPolicy}.
 */
public record RemoteCallPolicyContext(
        String method,
        String targetService,
        String serviceId,
        String path,
        String url,
        String callerId
) {
}
