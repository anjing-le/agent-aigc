package com.anjing.client;

/**
 * Observation hook for completed outbound remote calls.
 *
 * <p>The scaffold ships a no-op implementation. Downstream services can replace
 * it with audit persistence, metrics, tracing, or governance dashboards without
 * changing remote call sites.</p>
 */
public interface RemoteCallObserver {

    /**
     * Called once after a remote call succeeds, fails, or is rejected by policy.
     */
    default void onComplete(RemoteCallObservation observation) {
    }
}
