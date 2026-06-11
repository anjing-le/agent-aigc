package com.anjing.client;

/**
 * Sanitized remote call audit payload.
 *
 * <p>It intentionally excludes request body, response body, and headers so
 * observers can forward the event to logs, metrics, or audit storage safely.</p>
 */
public record RemoteCallObservation(
        String method,
        String targetService,
        String serviceId,
        String path,
        String url,
        String callerId,
        String requestId,
        String traceId,
        String tenantId,
        String userId,
        String timeZone,
        String locale,
        boolean success,
        long durationMs,
        String errorCode,
        String errorMessage,
        String exceptionType
) {
}
