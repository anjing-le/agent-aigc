/* eslint-disable */
// Generated from contracts/platform-contract.json. Do not edit manually.
// Run: node scripts/generate-platform-contract-frontend.js

export const PLATFORM_CONTRACT = {
  "schemaVersion": 1,
  "apiPrefix": "/api",
  "responseEnvelope": {
    "successCode": "0",
    "fields": [
      "code",
      "message",
      "data",
      "timestamp",
      "requestId"
    ]
  },
  "pagination": {
    "fields": [
      "records",
      "current",
      "size",
      "total"
    ],
    "firstPage": 1
  },
  "requestHeaders": {
    "requestId": "X-Request-Id",
    "traceId": "X-Trace-Id",
    "tenantId": "X-Tenant-Id",
    "userId": "X-User-Id",
    "userName": "X-User-Name",
    "userRoles": "X-User-Roles",
    "callerId": "X-Caller-Id",
    "timeZone": "X-Time-Zone",
    "acceptLanguage": "Accept-Language"
  },
  "frontendPropagatedHeaders": [
    "requestId",
    "traceId",
    "timeZone",
    "acceptLanguage"
  ],
  "backendPropagatedHeaders": [
    "requestId",
    "traceId",
    "tenantId",
    "userId",
    "userName",
    "userRoles",
    "callerId",
    "timeZone",
    "acceptLanguage"
  ],
  "time": {
    "defaultTimeZone": "UTC",
    "serverCurrentTimeSource": "DateUtils",
    "clientTimeZoneHeader": "X-Time-Zone",
    "localeHeader": "Accept-Language"
  },
  "locale": {
    "defaultLocale": "zh-CN",
    "supportedLocales": [
      "zh-CN",
      "en-US"
    ],
    "clientLocaleHeader": "Accept-Language"
  },
  "errorCodeRanges": [
    {
      "range": "0",
      "name": "success"
    },
    {
      "range": "1000-1499",
      "name": "system"
    },
    {
      "range": "1500-1599",
      "name": "infrastructure"
    },
    {
      "range": "1600-1899",
      "name": "remote"
    },
    {
      "range": "1900-1999",
      "name": "workflow"
    },
    {
      "range": "2000-2099",
      "name": "business-common"
    },
    {
      "range": "2100-2399",
      "name": "auth"
    },
    {
      "range": "2400-2999",
      "name": "business-module"
    },
    {
      "range": "3000-3999",
      "name": "validation"
    },
    {
      "range": "4000-4099",
      "name": "security/gateway"
    },
    {
      "range": "4100-4999",
      "name": "gateway/reserved"
    },
    {
      "range": "5000-7999",
      "name": "downstream-app"
    },
    {
      "range": "8000-8999",
      "name": "integration"
    },
    {
      "range": "9000-9999",
      "name": "platform-ops"
    }
  ],
  "retryableErrorCodeRanges": [
    "1800-1899"
  ]
} as const

export const API_SUCCESS_CODE = PLATFORM_CONTRACT.responseEnvelope.successCode
export const REQUEST_HEADERS = PLATFORM_CONTRACT.requestHeaders
export const FRONTEND_PROPAGATED_HEADER_KEYS = PLATFORM_CONTRACT.frontendPropagatedHeaders
export const BACKEND_PROPAGATED_HEADER_KEYS = PLATFORM_CONTRACT.backendPropagatedHeaders
export const DEFAULT_TIME_ZONE = PLATFORM_CONTRACT.time.defaultTimeZone
export const DEFAULT_LOCALE = PLATFORM_CONTRACT.locale.defaultLocale
export const SUPPORTED_LOCALES = PLATFORM_CONTRACT.locale.supportedLocales

export type PlatformContract = typeof PLATFORM_CONTRACT
export type PlatformRequestHeaderKey = keyof typeof REQUEST_HEADERS
export type PlatformFrontendPropagatedHeaderKey = (typeof FRONTEND_PROPAGATED_HEADER_KEYS)[number]
export type PlatformBackendPropagatedHeaderKey = (typeof BACKEND_PROPAGATED_HEADER_KEYS)[number]
export type PlatformSupportedLocale = (typeof SUPPORTED_LOCALES)[number]
