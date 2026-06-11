package com.anjing.client;

/**
 * Lightweight policy hook for outbound remote calls.
 *
 * <p>The scaffold ships a no-op implementation. Downstream services can replace
 * it with circuit breaker, rate limit, gray routing, or governance policies
 * without changing remote call sites.</p>
 */
public interface RemoteCallPolicy {

    /**
     * Called once before the outbound call starts.
     */
    default void beforeCall(RemoteCallPolicyContext context) {
    }

    /**
     * Called when an outbound transport attempt succeeds.
     */
    default void afterSuccess(RemoteCallPolicyContext context) {
    }

    /**
     * Called when an outbound transport attempt fails.
     */
    default void afterFailure(RemoteCallPolicyContext context, RuntimeException exception) {
    }
}
