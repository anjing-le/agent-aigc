package com.anjing.model.constants;

/**
 * Request header names used by frontend, backend, and future service calls.
 */
public final class RequestHeaderConstants {

    public static final String REQUEST_ID = PlatformContractConstants.Headers.REQUEST_ID;
    public static final String TRACE_ID = PlatformContractConstants.Headers.TRACE_ID;
    public static final String TENANT_ID = PlatformContractConstants.Headers.TENANT_ID;
    public static final String USER_ID = PlatformContractConstants.Headers.USER_ID;
    public static final String USER_NAME = PlatformContractConstants.Headers.USER_NAME;
    public static final String USER_ROLES = PlatformContractConstants.Headers.USER_ROLES;
    public static final String CALLER_ID = PlatformContractConstants.Headers.CALLER_ID;
    public static final String TIME_ZONE = PlatformContractConstants.Headers.TIME_ZONE;
    public static final String ACCEPT_LANGUAGE = PlatformContractConstants.Headers.ACCEPT_LANGUAGE;

    private RequestHeaderConstants() {
    }
}
