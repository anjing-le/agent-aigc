#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const serviceBoundariesPath = path.join(root, 'contracts/service-boundaries.json')
const platformContractPath = path.join(root, 'contracts/platform-contract.json')

const files = {
  platform: 'backend/src/main/java/com/anjing/model/constants/PlatformContractConstants.java',
  properties: 'backend/src/main/java/com/anjing/config/properties/RemoteHttpClientProperties.java',
  request: 'backend/src/main/java/com/anjing/client/RemoteHttpRequest.java',
  client: 'backend/src/main/java/com/anjing/client/RemoteHttpClient.java',
  callerResolver: 'backend/src/main/java/com/anjing/client/RemoteCallerResolver.java',
  defaultCallerResolver: 'backend/src/main/java/com/anjing/client/DefaultRemoteCallerResolver.java',
  endpointResolver: 'backend/src/main/java/com/anjing/client/ServiceEndpointResolver.java',
  serviceEndpoint: 'backend/src/main/java/com/anjing/client/ServiceEndpoint.java',
  endpointRegistry: 'backend/src/main/java/com/anjing/client/ServiceEndpointRegistry.java',
  configuredEndpointRegistry: 'backend/src/main/java/com/anjing/client/ConfiguredServiceEndpointRegistry.java',
  configuredEndpointResolver: 'backend/src/main/java/com/anjing/client/ConfiguredServiceEndpointResolver.java',
  callPolicy: 'backend/src/main/java/com/anjing/client/RemoteCallPolicy.java',
  callPolicyContext: 'backend/src/main/java/com/anjing/client/RemoteCallPolicyContext.java',
  configuredCallPolicy: 'backend/src/main/java/com/anjing/client/ConfiguredRemoteCallPolicy.java',
  noopCallPolicy: 'backend/src/main/java/com/anjing/client/NoopRemoteCallPolicy.java',
  callObserver: 'backend/src/main/java/com/anjing/client/RemoteCallObserver.java',
  callObservation: 'backend/src/main/java/com/anjing/client/RemoteCallObservation.java',
  noopCallObserver: 'backend/src/main/java/com/anjing/client/NoopRemoteCallObserver.java',
  httpClientConfig: 'backend/src/main/java/com/anjing/config/http/RemoteHttpClientConfig.java',
  remoteWrapper: 'backend/src/main/java/com/anjing/util/RemoteCallWrapper.java',
  application: 'backend/src/main/resources/application.yml',
  example: 'backend/src/main/java/com/anjing/example/RemoteCallExampleService.java',
  test: 'backend/src/test/java/com/anjing/client/RemoteHttpClientTest.java',
  guide: 'project_document/REMOTE_CALL_GUIDE.md'
}

function fail(message) {
  console.error(`check-remote-http-contract: ${message}`)
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

requireToken(files.properties, 'private Map<String, String> serviceBaseUrls')
requireToken(files.properties, 'ServiceBoundaryConstants.APPLICATION_ID')
requireToken(files.properties, 'private Policy policy')
requireToken(files.properties, 'private boolean enabled')
requireToken(files.properties, 'private List<String> blockedServiceIds')
requireToken(files.properties, 'private List<String> allowedCallerIds')
requireToken(files.properties, 'private Map<String, List<String>> allowedCallerIdsByService')
requireToken(files.request, 'private String serviceId')
requireToken(files.request, 'private String path')
requireToken(files.client, 'getFromService')
requireToken(files.client, 'postToService')
requireToken(files.client, 'ParameterizedTypeReference')
requireToken(files.client, 'exchange(RemoteHttpRequest request, ParameterizedTypeReference<R> responseType)')
requireToken(files.client, 'responseSpec.body(responseType)')
requireToken(files.client, 'resolveUrl')
requireToken(files.client, 'RemoteCallerResolver')
requireToken(files.client, 'remoteCallerResolver.resolveCallerId')
requireToken(files.client, 'ServiceEndpointResolver')
requireToken(files.client, 'serviceEndpointResolver.resolveUrl')
requireToken(files.callerResolver, 'interface RemoteCallerResolver')
requireToken(files.callerResolver, 'resolveCallerId(RemoteHttpRequest request)')
requireToken(files.defaultCallerResolver, 'implements RemoteCallerResolver')
requireToken(files.defaultCallerResolver, 'request.getCallerId()')
requireToken(files.defaultCallerResolver, 'properties.getDefaultCallerId()')
requireToken(files.defaultCallerResolver, 'ServiceBoundaryConstants.APPLICATION_ID')
requireToken(files.client, 'RemoteCallPolicy')
requireToken(files.client, 'remoteCallPolicy.beforeCall')
requireToken(files.client, 'remoteCallPolicy.afterSuccess')
requireToken(files.client, 'remoteCallPolicy.afterFailure')
requireToken(files.client, 'buildPolicyContext')
requireToken(files.client, 'RemoteCallObserver')
requireToken(files.client, 'remoteCallObserver.onComplete')
requireToken(files.client, 'observeRemoteCall')
requireToken(files.client, 'RemoteCallObservation')
requireToken(files.client, 'durationMs')
requireToken(files.endpointResolver, 'interface ServiceEndpointResolver')
requireToken(files.endpointResolver, 'resolveUrl(String serviceId, String path)')
requireToken(files.serviceEndpoint, 'record ServiceEndpoint')
requireToken(files.serviceEndpoint, 'String baseUrl')
requireToken(files.serviceEndpoint, 'String source')
requireToken(files.endpointRegistry, 'interface ServiceEndpointRegistry')
requireToken(files.endpointRegistry, 'Optional<ServiceEndpoint> findEndpoint(String serviceId)')
requireToken(files.configuredEndpointRegistry, 'implements ServiceEndpointRegistry')
requireToken(files.configuredEndpointRegistry, 'properties.getServiceBaseUrls()')
requireToken(files.configuredEndpointRegistry, 'new ServiceEndpoint(serviceId, baseUrl, SOURCE)')
requireToken(files.configuredEndpointResolver, 'implements ServiceEndpointResolver')
requireToken(files.configuredEndpointResolver, 'ServiceEndpointRegistry')
requireToken(files.configuredEndpointResolver, 'serviceEndpointRegistry.findEndpoint(serviceId)')
requireToken(files.configuredEndpointResolver, 'joinUrl')
requireToken(files.callPolicy, 'interface RemoteCallPolicy')
requireToken(files.callPolicy, 'beforeCall(RemoteCallPolicyContext context)')
requireToken(files.callPolicy, 'afterSuccess(RemoteCallPolicyContext context)')
requireToken(files.callPolicy, 'afterFailure(RemoteCallPolicyContext context, RuntimeException exception)')
requireToken(files.callPolicyContext, 'record RemoteCallPolicyContext')
requireToken(files.configuredCallPolicy, 'implements RemoteCallPolicy')
requireToken(files.configuredCallPolicy, 'properties.getPolicy()')
requireToken(files.configuredCallPolicy, 'REMOTE_CALL_PERMISSION_DENIED')
requireToken(files.configuredCallPolicy, 'getBlockedServiceIds()')
requireToken(files.configuredCallPolicy, 'getAllowedCallerIdsByService()')
requireToken(files.noopCallPolicy, 'implements RemoteCallPolicy')
requireToken(files.callObserver, 'interface RemoteCallObserver')
requireToken(files.callObserver, 'onComplete(RemoteCallObservation observation)')
requireToken(files.callObservation, 'record RemoteCallObservation')
requireToken(files.callObservation, 'String requestId')
requireToken(files.callObservation, 'String traceId')
requireToken(files.callObservation, 'long durationMs')
requireToken(files.callObservation, 'String errorCode')
requireToken(files.noopCallObserver, 'implements RemoteCallObserver')
requireToken(files.httpClientConfig, '@ConditionalOnMissingBean(RemoteCallPolicy.class)')
requireToken(files.httpClientConfig, 'new ConfiguredRemoteCallPolicy(properties)')
requireToken(files.httpClientConfig, '@ConditionalOnMissingBean(RemoteCallObserver.class)')
requireToken(files.httpClientConfig, 'new NoopRemoteCallObserver()')
requireToken(files.httpClientConfig, '@ConditionalOnMissingBean(RemoteCallerResolver.class)')
requireToken(files.httpClientConfig, 'new DefaultRemoteCallerResolver(properties)')
requireToken(files.httpClientConfig, '@ConditionalOnMissingBean(ServiceEndpointRegistry.class)')
requireToken(files.httpClientConfig, 'new ConfiguredServiceEndpointRegistry(properties)')
requireToken(files.application, 'service-base-urls:')
requireToken(files.application, 'REMOTE_HTTP_POLICY_ENABLED')
requireToken(files.application, 'blocked-service-ids:')
requireToken(files.application, 'allowed-caller-ids:')
requireToken(files.platform, 'BACKEND_PROPAGATED_HEADER_KEYS')
requireToken(files.remoteWrapper, 'PlatformContractConstants.BACKEND_PROPAGATED_HEADER_KEYS')
requireToken(files.remoteWrapper, 'appendContextHeader')
requireToken(files.example, 'ServiceBoundaryConstants.APPLICATION_ID')
requireToken(files.example, '.serviceId(ServiceBoundaryConstants.APPLICATION_ID)')
requireToken(files.example, '.path(ApiConstants.Test.PING_FULL)')
requireToken(files.example, 'new ParameterizedTypeReference<APIResponse<String>>()')
requireToken(files.test, 'APIResponse<PageResult<ItemView>>')
requireToken(files.test, 'MockRestServiceServer')
requireToken(files.test, 'ParameterizedTypeReference<APIResponse<PageResult<ItemView>>>')
requireToken(files.test, 'ConfiguredServiceEndpointResolver')
requireToken(files.test, 'configuredEndpointResolverShouldResolveServiceIdAndPath')
requireToken(files.test, 'exchangeShouldApplyRemoteCallPolicyBeforeRequest')
requireToken(files.test, 'REMOTE_CALL_CIRCUIT_BREAKER_OPEN')
requireToken(files.test, 'configuredRemoteCallPolicyShouldRejectBlockedService')
requireToken(files.test, 'configuredRemoteCallPolicyShouldEnforceGlobalCallerAllowList')
requireToken(files.test, 'configuredRemoteCallPolicyShouldEnforceServiceCallerAllowList')
requireToken(files.test, 'REMOTE_CALL_PERMISSION_DENIED')
requireToken(files.test, 'RecordingRemoteCallPolicy')
requireToken(files.test, 'RecordingRemoteCallObserver')
requireToken(files.test, 'observer.observation.durationMs()')
requireToken(files.test, 'exchangeShouldUseRemoteCallerResolver')
requireToken(files.test, 'defaultRemoteCallerResolverShouldSupportRequestOverrideAndFallback')
requireToken(files.test, 'configuredEndpointResolverShouldUseServiceEndpointRegistry')
requireToken(files.test, 'configuredServiceEndpointRegistryShouldReadConfiguredBaseUrls')
requireToken(files.guide, 'ServiceBoundaryConstants.Auth.OWNER')
requireToken(files.guide, '.path(ApiConstants.Auth.ME_FULL)')
requireToken(files.guide, 'ServiceBoundaryConstants.APPLICATION_ID')
requireToken(files.guide, 'new ParameterizedTypeReference<APIResponse<CurrentUserResponse>>()')
requireToken(files.guide, 'service-base-urls:')
requireToken(files.guide, 'RemoteCallerResolver')
requireToken(files.guide, 'ServiceEndpointRegistry')
requireToken(files.guide, 'ConfiguredRemoteCallPolicy')
requireToken(files.guide, 'REMOTE_HTTP_POLICY_ENABLED')
requireToken(files.guide, 'allowed-caller-ids-by-service')

let serviceBoundaries
try {
  serviceBoundaries = JSON.parse(fs.readFileSync(serviceBoundariesPath, 'utf8'))
} catch (error) {
  fail(`invalid contracts/service-boundaries.json: ${error.message}`)
}

let platformContract
try {
  platformContract = JSON.parse(fs.readFileSync(platformContractPath, 'utf8'))
} catch (error) {
  fail(`invalid contracts/platform-contract.json: ${error.message}`)
}

const applicationId = serviceBoundaries.applicationId
if (!applicationId) {
  fail('contracts/service-boundaries.json must define applicationId')
}

requireToken(files.application, `${applicationId}:`)
requireToken(files.example, 'ServiceBoundaryConstants.APPLICATION_ID')

const sampleSource = read(files.example)
if (/\.url\("http:\/\/localhost/.test(sampleSource) || sampleSource.includes('serverPort + ApiConstants')) {
  fail('RemoteCallExampleService should use serviceId + path instead of composing localhost URLs')
}

const guideSource = read(files.guide)
if (guideSource.includes('.url("http://infra-auth')) {
  fail('REMOTE_CALL_GUIDE should demonstrate serviceId + path for internal service calls')
}

const requestHeaders = platformContract.requestHeaders || {}
const backendPropagatedHeaders = platformContract.backendPropagatedHeaders || []
if (!backendPropagatedHeaders.length) {
  fail('contracts/platform-contract.json must define backendPropagatedHeaders')
}
for (const key of backendPropagatedHeaders) {
  if (!requestHeaders[key]) {
    fail(`backendPropagatedHeaders contains unknown request header key: ${key}`)
  }
  requireToken(files.remoteWrapper, `case "${key}"`)
  requireToken(files.guide, requestHeaders[key])
}

console.log('check-remote-http-contract: ok')
