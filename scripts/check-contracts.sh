#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

fail() {
  echo "check-contracts: $*" >&2
  exit 1
}

require_file() {
  local file="$1"
  [[ -f "$file" ]] || fail "missing required file: $file"
}

require_token() {
  local file="$1"
  local token="$2"
  rg -q --fixed-strings "$token" "$file" \
    || fail "$file is missing required token: $token"
}

require_absent() {
  local pattern="$1"
  shift
  if rg -n "$pattern" "$@"; then
    fail "contract violation found for pattern: $pattern"
  fi
}

require_file backend/src/main/java/com/anjing/model/constants/ApiConstants.java
require_file backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java
require_file backend/src/main/java/com/anjing/model/constants/ServiceBoundaryConstants.java
require_file frontend/src/api/paths.ts
require_file frontend/src/api/openapiClient.ts
require_file backend/src/main/java/com/anjing/model/response/APIResponse.java
require_file backend/src/main/java/com/anjing/model/response/PageResult.java
require_file frontend/src/utils/http/response.ts
require_file backend/src/main/java/com/anjing/model/constants/RequestHeaderConstants.java
require_file backend/src/main/java/com/anjing/config/http/RequestContextFilter.java
require_file backend/src/main/java/com/anjing/util/LocaleUtils.java
require_file backend/src/main/java/com/anjing/util/TimeZoneUtils.java
require_file frontend/src/utils/http/context.ts
require_file frontend/src/utils/locale/index.ts
require_file backend/src/main/java/com/anjing/client/RemoteHttpClient.java
require_file backend/src/main/java/com/anjing/client/RemoteCallerResolver.java
require_file backend/src/main/java/com/anjing/client/DefaultRemoteCallerResolver.java
require_file backend/src/main/java/com/anjing/client/ServiceEndpointResolver.java
require_file backend/src/main/java/com/anjing/client/ServiceEndpoint.java
require_file backend/src/main/java/com/anjing/client/ServiceEndpointRegistry.java
require_file backend/src/main/java/com/anjing/client/ConfiguredServiceEndpointRegistry.java
require_file backend/src/main/java/com/anjing/client/ConfiguredServiceEndpointResolver.java
require_file backend/src/main/java/com/anjing/client/RemoteCallPolicy.java
require_file backend/src/main/java/com/anjing/client/RemoteCallPolicyContext.java
require_file backend/src/main/java/com/anjing/client/ConfiguredRemoteCallPolicy.java
require_file backend/src/main/java/com/anjing/client/NoopRemoteCallPolicy.java
require_file backend/src/main/java/com/anjing/client/RemoteCallObserver.java
require_file backend/src/main/java/com/anjing/client/RemoteCallObservation.java
require_file backend/src/main/java/com/anjing/client/NoopRemoteCallObserver.java
require_file backend/src/main/java/com/anjing/util/RemoteCallWrapper.java
require_file frontend/src/contracts/platform-contract.ts
require_file frontend/src/contracts/service-boundaries.ts
require_file frontend/src/contracts/openapi/schemas.ts
require_file frontend/src/contracts/openapi/operations.ts
require_file frontend/src/utils/time/index.ts
require_file contracts/platform-contract.json
require_file contracts/service-boundaries.json
require_file project_document/API_CONTRACT_GUIDE.md
require_file project_document/API_PATH_GUIDE.md
require_file project_document/PLATFORM_CONTRACT_GUIDE.md
require_file project_document/OPENAPI_CONTRACT_GUIDE.md
require_file project_document/SERVICE_BOUNDARY_GUIDE.md
require_file project_document/REMOTE_CALL_GUIDE.md
require_file project_document/ENVIRONMENT_PROFILE_GUIDE.md
require_file project_document/FEATURE_STATUS_GUIDE.md
require_file project_document/LOCAL_STARTUP_GUIDE.md
require_file project_document/SHARED_KERNEL_GUIDE.md
require_file scripts/check-api-constants.js
require_file scripts/check-api-path-parity.js
require_file scripts/check-backend-controller-contracts.js
require_file scripts/check-frontend-api-boundaries.js
require_file scripts/check-frontend-openapi-boundaries.js
require_file scripts/check-frontend-context-contract.js
require_file scripts/check-backend-context-contract.js
require_file scripts/check-async-context-contract.js
require_file scripts/check-frontend-time-contract.js
require_file scripts/generate-platform-contract-backend.js
require_file scripts/generate-platform-contract-frontend.js
require_file scripts/generate-service-boundaries-backend.js
require_file scripts/generate-service-boundaries-frontend.js
require_file scripts/check-platform-contract.js
require_file scripts/check-error-codes.js
require_file scripts/check-openapi-contract.js
require_file scripts/check-openapi-runtime-contract.js
require_file scripts/generate-openapi-frontend-types.js
require_file scripts/check-service-boundaries.js
require_file scripts/check-shared-kernel.js
require_file scripts/check-remote-http-contract.js

# URL contract: backend Controller mappings use ApiConstants; frontend API modules use ApiPaths.
require_absent '@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\(\s*"/api' \
  backend/src/main/java \
  --glob '*Controller.java' \
  --glob '!backend/target/**'

require_absent 'url:\s*['"'"'"]/api' \
  frontend/src/api \
  --glob '*.ts' \
  --glob '!paths.ts'

require_absent 'request\.(get|post|put|del|delete)\([^)]*['"'"'"]/api' \
  frontend/src \
  --glob '*.ts' \
  --glob '*.vue' \
  --glob '!frontend/src/api/paths.ts'

require_absent 'VITE_API_URL.*\/api' \
  frontend/src \
  --glob '*.ts' \
  --glob '*.vue' \
  --glob '!frontend/src/api/paths.ts'

require_token frontend/src/api/paths.ts 'resolveApiPath'
require_token frontend/src/api/paths.ts 'uploadWangEditor'
require_token frontend/src/api/paths.ts 'SERVICE_BOUNDARY_ROUTE_PATHS'
require_token frontend/src/api/openapiClient.ts 'OPENAPI_OPERATIONS'
require_token frontend/src/api/openapiClient.ts 'OpenApiOperationPathParams'
require_token frontend/src/api/openapiClient.ts 'OpenApiOperationQuery'
require_token frontend/src/api/openapiClient.ts 'openApiRequest'
require_token frontend/src/api/openapiClient.ts 'resolveOpenApiPath'
require_token frontend/src/contracts/service-boundaries.ts 'SERVICE_BOUNDARY_ROUTE_PATHS'

# Response contract: new code uses message/code/data; msg compatibility is centralized.
require_absent '\bmsg\??:' \
  frontend/src \
  --glob '*.ts' \
  --glob '!frontend/src/utils/http/response.ts' \
  --glob '!frontend/src/utils/http/error.ts' \
  --glob '!frontend/src/types/common/response.ts'

require_token backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java 'public static final String SUCCESS_CODE = "0"'
require_token backend/src/main/java/com/anjing/model/response/APIResponse.java 'PlatformContractConstants.Response.SUCCESS_CODE'
require_token backend/src/main/java/com/anjing/model/response/APIResponse.java 'private String message'
require_token backend/src/main/java/com/anjing/model/response/APIResponse.java 'private String requestId'
require_token backend/src/main/java/com/anjing/model/response/APIResponse.java 'successData'
require_token backend/src/main/java/com/anjing/model/response/APIResponse.java 'successMessage'
require_token backend/src/main/java/com/anjing/model/response/PageResult.java 'private List<T> records'
require_token backend/src/main/java/com/anjing/model/response/PageResult.java 'current'
require_token backend/src/main/java/com/anjing/model/response/PageResult.java 'size'
require_token backend/src/main/java/com/anjing/model/response/PageResult.java 'total'
require_token frontend/src/contracts/platform-contract.ts 'API_SUCCESS_CODE = PLATFORM_CONTRACT.responseEnvelope.successCode'
require_token frontend/src/utils/http/response.ts 'import { API_SUCCESS_CODE }'
require_token frontend/src/utils/table/tableConfig.ts 'records'

# Context and remote-call contract: request identity, locale, timezone and caller identity are centralized.
for header in \
  'X-Request-Id' \
  'X-Trace-Id' \
  'X-Tenant-Id' \
  'X-User-Id' \
  'X-Time-Zone' \
  'Accept-Language'
do
  require_token backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java "$header"
done
require_token backend/src/main/java/com/anjing/model/constants/RequestHeaderConstants.java 'PlatformContractConstants.Headers.REQUEST_ID'
require_token backend/src/main/java/com/anjing/model/constants/RequestHeaderConstants.java 'PlatformContractConstants.Headers.TRACE_ID'
require_token backend/src/main/java/com/anjing/model/constants/RequestHeaderConstants.java 'PlatformContractConstants.Headers.TIME_ZONE'
require_token backend/src/main/java/com/anjing/model/constants/RequestHeaderConstants.java 'PlatformContractConstants.Headers.ACCEPT_LANGUAGE'

require_token backend/src/main/java/com/anjing/config/http/RequestContextFilter.java 'GlobalRequestContextHolder.set(context)'
require_token backend/src/main/java/com/anjing/config/http/RequestContextFilter.java 'LocaleUtils.normalizeAcceptLanguage'
require_token backend/src/main/java/com/anjing/config/http/RequestContextFilter.java 'TimeZoneUtils.normalizeTimeZone'
require_token backend/src/main/java/com/anjing/config/http/RequestContextFilter.java 'response.setHeader(RequestHeaderConstants.REQUEST_ID'
require_token backend/src/main/java/com/anjing/aspect/ControllerLogAspect.java 'API_REQUEST_END'
require_token backend/src/main/java/com/anjing/aspect/ControllerLogAspect.java 'durationMs={}'
require_token backend/src/main/java/com/anjing/aspect/ControllerLogAspect.java 'errorCode={}'
require_token backend/src/main/java/com/anjing/config/async/AsyncConfig.java 'setTaskDecorator(requestContextTaskDecorator)'
require_token backend/src/main/java/com/anjing/config/async/RequestContextTaskDecorator.java 'MDC.getCopyOfContextMap()'
require_token frontend/src/contracts/platform-contract.ts '"timeZone": "X-Time-Zone"'
require_token frontend/src/contracts/platform-contract.ts '"acceptLanguage": "Accept-Language"'
require_token frontend/src/contracts/platform-contract.ts 'FRONTEND_PROPAGATED_HEADER_KEYS = PLATFORM_CONTRACT.frontendPropagatedHeaders'
require_token frontend/src/contracts/platform-contract.ts 'BACKEND_PROPAGATED_HEADER_KEYS = PLATFORM_CONTRACT.backendPropagatedHeaders'
require_token frontend/src/contracts/platform-contract.ts 'DEFAULT_LOCALE = PLATFORM_CONTRACT.locale.defaultLocale'
require_token frontend/src/contracts/platform-contract.ts 'SUPPORTED_LOCALES = PLATFORM_CONTRACT.locale.supportedLocales'
require_token frontend/src/utils/locale/index.ts 'matchSupportedLocale'
require_token frontend/src/utils/locale/index.ts 'getClientLocale'
require_token frontend/src/utils/locale/index.ts 'getLanguageTag'
require_token frontend/src/utils/http/context.ts 'getOrCreateTraceId'
require_token frontend/src/utils/http/context.ts 'buildRequestContext'
require_token frontend/src/utils/http/context.ts 'buildRequestContextHeaders'
require_token frontend/src/utils/http/context.ts "import { getLanguageTag } from '@/utils/locale'"
require_token frontend/src/utils/http/context.ts 'REQUEST_HEADERS[key]'
require_token backend/src/main/java/com/anjing/util/RemoteCallWrapper.java 'serviceCallHeaders'
require_token backend/src/main/java/com/anjing/util/RemoteCallWrapper.java 'PlatformContractConstants.BACKEND_PROPAGATED_HEADER_KEYS'
require_token backend/src/main/java/com/anjing/util/RemoteCallWrapper.java 'RequestHeaderConstants.CALLER_ID'
require_token backend/src/main/java/com/anjing/client/RemoteHttpClient.java 'RemoteCallWrapper.callWithRetry'
require_token backend/src/main/java/com/anjing/client/RemoteHttpClient.java 'resolveUrl'
require_token backend/src/main/java/com/anjing/client/RemoteHttpRequest.java 'private String serviceId'
require_token backend/src/main/java/com/anjing/config/properties/RemoteHttpClientProperties.java 'serviceBaseUrls'

# Time contract: shared utilities exist; avoid system default timezone in backend business code.
require_token frontend/src/utils/time/index.ts 'getClientTimeZone'
require_token frontend/src/utils/time/index.ts 'getClientLocale'
require_token frontend/src/utils/time/index.ts 'import { DEFAULT_TIME_ZONE }'
require_token frontend/src/utils/time/index.ts 'formatDateTime'
require_token backend/src/main/java/com/anjing/util/TimeZoneUtils.java 'PlatformContractConstants.Time.DEFAULT_TIME_ZONE'
require_token backend/src/main/java/com/anjing/util/TimeZoneUtils.java 'normalizeTimeZone'
require_token backend/src/main/java/com/anjing/util/DateUtils.java 'TimeZoneUtils.defaultZoneId()'
require_token backend/src/main/java/com/anjing/util/DateUtils.java 'nowIso'
require_token backend/src/main/java/com/anjing/util/DateUtils.java 'nowEpochMilli'
require_absent 'ZoneId\.systemDefault\(' \
  backend/src/main/java \
  --glob '*.java'

require_absent '\b(Instant|LocalDateTime|OffsetDateTime|ZonedDateTime)\.now\(' \
  backend/src/main/java \
  --glob '*.java' \
  --glob '!backend/src/main/java/com/anjing/util/DateUtils.java'

node scripts/check-api-constants.js
node scripts/check-api-path-parity.js
node scripts/check-backend-controller-contracts.js
node scripts/check-frontend-api-boundaries.js
node scripts/check-frontend-openapi-boundaries.js
node scripts/check-frontend-context-contract.js
node scripts/check-backend-context-contract.js
node scripts/check-async-context-contract.js
node scripts/check-frontend-time-contract.js
node scripts/generate-platform-contract-backend.js --check
node scripts/generate-platform-contract-frontend.js --check
node scripts/generate-service-boundaries-backend.js --check
node scripts/generate-service-boundaries-frontend.js --check
node scripts/check-platform-contract.js
node scripts/check-error-codes.js
node scripts/check-openapi-contract.js
node scripts/check-service-boundaries.js
node scripts/check-shared-kernel.js
node scripts/check-remote-http-contract.js

echo "check-contracts: ok"
