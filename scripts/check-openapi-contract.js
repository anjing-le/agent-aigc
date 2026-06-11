#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')

const files = {
  pom: 'backend/pom.xml',
  app: 'backend/src/main/resources/application.yml',
  dev: 'backend/src/main/resources/application-dev.yml',
  test: 'backend/src/main/resources/application-test.yml',
  prod: 'backend/src/main/resources/application-prod.yml',
  applicationClass: 'backend/src/main/java/com/anjing/Application.java',
  config: 'backend/src/main/java/com/anjing/config/openapi/OpenApiConfig.java',
  runtimeCheck: 'scripts/check-openapi-runtime-contract.js',
  frontendTypesGenerator: 'scripts/generate-openapi-frontend-types.js',
  backendProbe: 'scripts/probe-backend-dev.sh',
  authController: 'backend/src/main/java/com/anjing/controller/AuthController.java',
  testController: 'backend/src/main/java/com/anjing/controller/TestController.java',
  loginRequest: 'backend/src/main/java/com/anjing/model/request/LoginRequest.java',
  refreshRequest: 'backend/src/main/java/com/anjing/model/request/RefreshTokenRequest.java',
  tokenResponse: 'backend/src/main/java/com/anjing/model/response/AuthTokenResponse.java',
  currentUserResponse: 'backend/src/main/java/com/anjing/model/response/CurrentUserResponse.java',
  frontendGeneratedSchemas: 'frontend/src/contracts/openapi/schemas.ts',
  frontendGeneratedOperations: 'frontend/src/contracts/openapi/operations.ts',
  frontendOpenApiClient: 'frontend/src/api/openapiClient.ts',
  frontendAuthModel: 'frontend/src/api/model/authModel.ts',
  frontendAuthApi: 'frontend/src/api/auth.ts',
  frontendUserApi: 'frontend/src/api/user.ts',
  guide: 'project_document/OPENAPI_CONTRACT_GUIDE.md',
  status: 'project_document/STATUS.md'
}

function fail(message) {
  console.error(`check-openapi-contract: ${message}`)
  process.exit(1)
}

function read(relativeFile) {
  const file = path.join(root, relativeFile)
  if (!fs.existsSync(file)) {
    fail(`missing required file: ${relativeFile}`)
  }
  return fs.readFileSync(file, 'utf8')
}

function requireToken(relativeFile, token) {
  const source = read(relativeFile)
  if (!source.includes(token)) {
    fail(`${relativeFile} is missing token: ${token}`)
  }
}

function requireAbsent(relativeFile, pattern, description) {
  const source = read(relativeFile)
  if (pattern.test(source)) {
    fail(`${relativeFile} contains ${description}: ${pattern}`)
  }
}

requireToken(files.pom, '<springdoc-openapi.version>2.8.17</springdoc-openapi.version>')
requireToken(files.pom, '<artifactId>springdoc-openapi-starter-webmvc-api</artifactId>')
requireAbsent(files.pom, /springdoc-openapi-starter-webmvc-ui/, 'Swagger UI dependency')
requireToken(files.applicationClass, 'OpenAPI JSON')
requireAbsent(files.applicationClass, /swagger-ui/, 'Swagger UI startup link')

for (const token of [
  'springdoc:',
  'api-docs:',
  'path: /v3/api-docs',
  'packages-to-scan: com.anjing.controller',
  'paths-to-match: /api/**'
]) {
  requireToken(files.app, token)
}

requireToken(files.dev, 'enabled: ${OPENAPI_API_DOCS_ENABLED:true}')
requireToken(files.test, 'enabled: ${OPENAPI_API_DOCS_ENABLED:true}')
requireToken(files.prod, 'enabled: ${OPENAPI_API_DOCS_ENABLED:false}')

for (const token of [
  'GroupedOpenApi',
  'OperationCustomizer',
  'PlatformContractConstants.API_PREFIX + "/**"',
  'RequestHeaderConstants.REQUEST_ID',
  'RequestHeaderConstants.TRACE_ID',
  'RequestHeaderConstants.TENANT_ID',
  'RequestHeaderConstants.CALLER_ID',
  'RequestHeaderConstants.USER_NAME',
  'RequestHeaderConstants.USER_ROLES',
  'RequestHeaderConstants.TIME_ZONE',
  'RequestHeaderConstants.ACCEPT_LANGUAGE'
]) {
  requireToken(files.config, token)
}

for (const token of [
  'contracts/service-boundaries.json',
  'contracts/platform-contract.json',
  'expectedRuntimeRoutes',
  'ensureOperationHeaders',
  'check-openapi-runtime-contract: ok'
]) {
  requireToken(files.runtimeCheck, token)
}

requireToken(files.backendProbe, 'check-openapi-runtime-contract.js')
requireToken(files.backendProbe, 'generate-openapi-frontend-types.js')

for (const token of [
  'components?.schemas',
  'function renderOperations',
  'function operationParameters',
  'OpenApiOperationTypes',
  'OpenApiOperationPathParams',
  'OpenApiOperationQuery',
  'operationsOutputPath',
  'pathParamsType',
  'queryType',
  'function schemaType',
  'OpenApiSchemas',
  'generate-openapi-frontend-types: ok'
]) {
  requireToken(files.frontendTypesGenerator, token)
}

for (const token of [
  '@Tag(name = "Auth"',
  '@Operation(summary = "Login")',
  'APIResponse<AuthTokenResponse>',
  'APIResponse<CurrentUserResponse>',
  '@RequestBody @Validated RefreshTokenRequest'
]) {
  requireToken(files.authController, token)
}

requireAbsent(files.authController, /APIResponse<Map<String,\s*Object>>/, 'Map based auth response')
requireAbsent(files.authController, /@RequestBody\s+Map<String,\s*String>/, 'Map based refresh request')

requireToken(files.testController, '@Tag(name = "Scaffold Test"')
requireToken(files.loginRequest, '@Schema(description = "Login request")')
requireToken(files.refreshRequest, '@Schema(description = "Refresh token request")')
requireToken(files.tokenResponse, '@Schema(description = "Authentication token payload")')
requireToken(files.currentUserResponse, '@Schema(description = "Current authenticated user payload")')

for (const token of [
  'export interface LoginRequest',
  'username: string',
  'export interface AuthTokenResponse',
  'accessToken: string',
  'export interface CurrentUserResponse',
  'roles: string[]',
  'export interface OpenApiSchemas'
]) {
  requireToken(files.frontendGeneratedSchemas, token)
}

for (const token of [
  'export const OPENAPI_OPERATIONS',
  'login: {',
  'getCurrentUser: {',
  'refreshToken: {',
  'OpenApiOperationRequest',
  'OpenApiOperationData',
  'OpenApiOperationPathParams',
  'OpenApiOperationQuery',
  'pathParams: { id: number }',
  'query: { keyword?: string }',
  'Schemas.LoginRequest',
  'Schemas.APIResponseAuthTokenResponse',
  'Schemas.APIResponseCurrentUserResponse'
]) {
  requireToken(files.frontendGeneratedOperations, token)
}

for (const token of [
  "from '@/contracts/openapi/operations'",
  'OPENAPI_OPERATIONS',
  'OpenApiOperationPathParams',
  'OpenApiOperationQuery',
  'OpenApiOperationRequest',
  'OpenApiOperationData',
  'bindOpenApiPathParams',
  'openApiPath',
  'resolveOpenApiPath',
  'OpenApiOperationsWithRequiredOptions',
  'openApiRequest'
]) {
  requireToken(files.frontendOpenApiClient, token)
}

for (const token of [
  "from '@/contracts/openapi/operations'",
  "export type LoginParams = OpenApiOperationRequest<'login'>",
  "export type LoginResponse = OpenApiOperationData<'login'>",
  "export type UserInfo = OpenApiOperationData<'getCurrentUser'>",
  "export type RefreshTokenParams = OpenApiOperationRequest<'refreshToken'>"
]) {
  requireToken(files.frontendAuthModel, token)
}

for (const token of [
  "import { openApiRequest } from './openapiClient'",
  "openApiRequest('login'",
  "openApiRequest('getCurrentUser'",
  "openApiRequest('refreshToken'",
  'fetchRefreshToken'
]) {
  requireToken(files.frontendAuthApi, token)
}

for (const token of [
  "import { openApiRequest } from './openapiClient'",
  "fetchLogin(data)",
  "openApiRequest('logout'",
  "fetchGetUserInfo()"
]) {
  requireToken(files.frontendUserApi, token)
}
requireAbsent(files.frontendUserApi, /ApiPaths\.auth\.(login|logout|me)/, 'runtime auth path bypassing openApiRequest')

for (const token of [
  '/v3/api-docs',
  'springdoc-openapi-starter-webmvc-api',
  'OPENAPI_API_DOCS_ENABLED',
  'node scripts/check-openapi-contract.js',
  'node scripts/check-openapi-runtime-contract.js',
  'node scripts/generate-openapi-frontend-types.js',
  './scripts/probe-backend-dev.sh'
]) {
  requireToken(files.guide, token)
}

requireToken(files.status, 'node scripts/check-openapi-contract.js')
requireToken(files.status, '/v3/api-docs')

console.log('check-openapi-contract: ok')
