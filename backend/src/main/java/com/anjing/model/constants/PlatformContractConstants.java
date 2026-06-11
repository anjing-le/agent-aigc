package com.anjing.model.constants;

/**
 * Generated from contracts/platform-contract.json. Do not edit manually.
 * Run: node scripts/generate-platform-contract-backend.js
 */
public final class PlatformContractConstants {

    public static final int SCHEMA_VERSION = 1;
    public static final String API_PREFIX = "/api";
    public static final String[] FRONTEND_PROPAGATED_HEADER_KEYS = { "requestId", "traceId", "timeZone", "acceptLanguage" };
    public static final String[] BACKEND_PROPAGATED_HEADER_KEYS = { "requestId", "traceId", "tenantId", "userId", "userName", "userRoles", "callerId", "timeZone", "acceptLanguage" };

    private PlatformContractConstants() {
    }

    public static final class Response {
        public static final String SUCCESS_CODE = "0";
        public static final String[] FIELDS = { "code", "message", "data", "timestamp", "requestId" };

        private Response() {
        }
    }

    public static final class Pagination {
        public static final String[] FIELDS = { "records", "current", "size", "total" };
        public static final int FIRST_PAGE = 1;

        private Pagination() {
        }
    }

    public static final class Headers {
        public static final String REQUEST_ID = "X-Request-Id";
        public static final String TRACE_ID = "X-Trace-Id";
        public static final String TENANT_ID = "X-Tenant-Id";
        public static final String USER_ID = "X-User-Id";
        public static final String USER_NAME = "X-User-Name";
        public static final String USER_ROLES = "X-User-Roles";
        public static final String CALLER_ID = "X-Caller-Id";
        public static final String TIME_ZONE = "X-Time-Zone";
        public static final String ACCEPT_LANGUAGE = "Accept-Language";

        private Headers() {
        }
    }

    public static final class Time {
        public static final String DEFAULT_TIME_ZONE = "UTC";
        public static final String CLIENT_TIME_ZONE_HEADER = Headers.TIME_ZONE;
        public static final String LOCALE_HEADER = Headers.ACCEPT_LANGUAGE;

        private Time() {
        }
    }

    public static final class Locale {
        public static final String DEFAULT_LOCALE = "zh-CN";
        public static final String[] SUPPORTED_LOCALES = { "zh-CN", "en-US" };
        public static final String CLIENT_LOCALE_HEADER = Headers.ACCEPT_LANGUAGE;

        private Locale() {
        }
    }

    public static final class ErrorCodes {
        public static final String[] RANGES = { "0", "1000-1499", "1500-1599", "1600-1899", "1900-1999", "2000-2099", "2100-2399", "2400-2999", "3000-3999", "4000-4099", "4100-4999", "5000-7999", "8000-8999", "9000-9999" };
        public static final String[] RETRYABLE_RANGES = { "1800-1899" };

        private ErrorCodes() {
        }
    }
}
