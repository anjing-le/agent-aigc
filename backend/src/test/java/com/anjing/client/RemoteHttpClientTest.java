package com.anjing.client;

import com.anjing.config.properties.RemoteHttpClientProperties;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.constants.RequestHeaderConstants;
import com.anjing.model.constants.ServiceBoundaryConstants;
import com.anjing.model.errorcode.RemoteErrorCode;
import com.anjing.model.exception.SystemException;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.model.response.APIResponse;
import com.anjing.model.response.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemoteHttpClientTest {

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
    }

    @Test
    void exchangeShouldPreserveNestedGenericResponseType() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RemoteHttpClientProperties properties = properties();
        RemoteHttpClient client = new RemoteHttpClient(
                builder.build(),
                properties,
                endpointResolver(properties),
                new DefaultRemoteCallerResolver(properties),
                new NoopRemoteCallPolicy(),
                new NoopRemoteCallObserver()
        );

        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-1")
                .traceId("tid-1")
                .tenantId("tenant-a")
                .userId("user-a")
                .locale("zh-CN")
                .timeZone("UTC")
                .build());

        server.expect(requestTo("http://inventory.local/api/items"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(RequestHeaderConstants.REQUEST_ID, "rid-1"))
                .andExpect(header(RequestHeaderConstants.TRACE_ID, "tid-1"))
                .andExpect(header(RequestHeaderConstants.TENANT_ID, "tenant-a"))
                .andRespond(withSuccess("""
                        {
                          "code": "0",
                          "message": "ok",
                          "data": {
                            "records": [{ "name": "alpha" }],
                            "current": 1,
                            "size": 1,
                            "total": 1
                          },
                          "timestamp": 1700000000000,
                          "requestId": "rid-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        APIResponse<PageResult<ItemView>> response = client.exchange(
                RemoteHttpRequest.builder()
                        .serviceId("inventory")
                        .path("/api/items")
                        .build(),
                new ParameterizedTypeReference<APIResponse<PageResult<ItemView>>>() {
                }
        );

        assertEquals("0", response.getCode());
        assertEquals(1, response.getData().getRecords().size());
        assertInstanceOf(ItemView.class, response.getData().getRecords().get(0));
        assertEquals("alpha", response.getData().getRecords().get(0).getName());
        server.verify();
    }

    @Test
    void exchangeShouldApplyRemoteCallPolicyBeforeRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RemoteHttpClientProperties properties = properties();
        RecordingRemoteCallObserver observer = new RecordingRemoteCallObserver();
        RemoteHttpClient client = new RemoteHttpClient(
                builder.build(),
                properties,
                endpointResolver(properties),
                new DefaultRemoteCallerResolver(properties),
                new RemoteCallPolicy() {
                    @Override
                    public void beforeCall(RemoteCallPolicyContext context) {
                        throw new SystemException(
                                "远程调用策略拒绝: " + context.targetService(),
                                RemoteErrorCode.REMOTE_CALL_CIRCUIT_BREAKER_OPEN
                        );
                    }
                },
                observer
        );

        SystemException error = assertThrows(
                SystemException.class,
                () -> client.exchange(
                        RemoteHttpRequest.builder()
                                .serviceId("inventory")
                                .path("/api/items")
                                .build(),
                        String.class
                )
        );

        assertEquals(RemoteErrorCode.REMOTE_CALL_CIRCUIT_BREAKER_OPEN, error.getErrorCode());
        assertEquals(1, observer.completeCount);
        assertFalse(observer.observation.success());
        assertEquals("inventory", observer.observation.targetService());
        assertEquals("/api/items", observer.observation.path());
        assertEquals(RemoteErrorCode.REMOTE_CALL_CIRCUIT_BREAKER_OPEN.getCode(), observer.observation.errorCode());
        assertEquals("SystemException", observer.observation.exceptionType());
        server.verify();
    }

    @Test
    void exchangeShouldRecordRemoteCallPolicySuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RemoteHttpClientProperties properties = properties();
        RecordingRemoteCallPolicy policy = new RecordingRemoteCallPolicy();
        RecordingRemoteCallObserver observer = new RecordingRemoteCallObserver();
        RemoteHttpClient client = new RemoteHttpClient(
                builder.build(),
                properties,
                endpointResolver(properties),
                new DefaultRemoteCallerResolver(properties),
                policy,
                observer
        );

        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-observe")
                .traceId("tid-observe")
                .tenantId("tenant-observe")
                .userId("user-observe")
                .locale("zh-CN")
                .timeZone("UTC")
                .build());

        server.expect(requestTo("http://inventory.local/api/items"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        String response = client.exchange(
                RemoteHttpRequest.builder()
                        .serviceId("inventory")
                        .path("/api/items")
                        .build(),
                String.class
        );

        assertEquals("ok", response);
        assertEquals(1, policy.beforeCount);
        assertEquals(1, policy.successCount);
        assertEquals(0, policy.failureCount);
        assertEquals("GET", policy.context.method());
        assertEquals("inventory", policy.context.targetService());
        assertEquals("/api/items", policy.context.path());
        assertEquals("http://inventory.local/api/items", policy.context.url());
        assertEquals(1, observer.completeCount);
        assertTrue(observer.observation.success());
        assertEquals("GET", observer.observation.method());
        assertEquals("inventory", observer.observation.targetService());
        assertEquals("/api/items", observer.observation.path());
        assertEquals("http://inventory.local/api/items", observer.observation.url());
        assertEquals("infra-dev-scaffolding-test", observer.observation.callerId());
        assertEquals("rid-observe", observer.observation.requestId());
        assertEquals("tid-observe", observer.observation.traceId());
        assertEquals("tenant-observe", observer.observation.tenantId());
        assertEquals("user-observe", observer.observation.userId());
        assertEquals("UTC", observer.observation.timeZone());
        assertEquals("zh-CN", observer.observation.locale());
        assertTrue(observer.observation.durationMs() >= 0);
        assertNull(observer.observation.errorCode());
        assertNull(observer.observation.exceptionType());
        server.verify();
    }

    @Test
    void exchangeShouldUseRemoteCallerResolver() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RemoteHttpClientProperties properties = properties();
        RecordingRemoteCallPolicy policy = new RecordingRemoteCallPolicy();
        RecordingRemoteCallObserver observer = new RecordingRemoteCallObserver();
        RemoteHttpClient client = new RemoteHttpClient(
                builder.build(),
                properties,
                endpointResolver(properties),
                request -> "gateway-caller",
                policy,
                observer
        );

        server.expect(requestTo("http://inventory.local/api/items"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(RequestHeaderConstants.CALLER_ID, "gateway-caller"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        String response = client.exchange(
                RemoteHttpRequest.builder()
                        .serviceId("inventory")
                        .path("/api/items")
                        .build(),
                String.class
        );

        assertEquals("ok", response);
        assertEquals("gateway-caller", policy.context.callerId());
        assertEquals("gateway-caller", observer.observation.callerId());
        server.verify();
    }

    @Test
    void defaultRemoteCallerResolverShouldSupportRequestOverrideAndFallback() {
        RemoteHttpClientProperties properties = properties();
        DefaultRemoteCallerResolver resolver = new DefaultRemoteCallerResolver(properties);

        assertEquals("request-caller", resolver.resolveCallerId(RemoteHttpRequest.builder()
                .callerId("request-caller")
                .build()));
        assertEquals("infra-dev-scaffolding-test", resolver.resolveCallerId(RemoteHttpRequest.builder().build()));

        properties.setDefaultCallerId("");
        assertEquals(ServiceBoundaryConstants.APPLICATION_ID, resolver.resolveCallerId(RemoteHttpRequest.builder().build()));
    }

    @Test
    void configuredEndpointResolverShouldResolveServiceIdAndPath() {
        ConfiguredServiceEndpointResolver resolver = endpointResolver(properties());

        assertEquals("http://inventory.local/api/items", resolver.resolveUrl("inventory", "/api/items"));
        assertEquals("http://inventory.local/api/items", resolver.resolveUrl("inventory", "api/items"));
        assertEquals("http://inventory.local", resolver.resolveUrl("inventory", null));
    }

    @Test
    void configuredEndpointResolverShouldRejectUnknownService() {
        ConfiguredServiceEndpointResolver resolver = endpointResolver(properties());

        SystemException error = assertThrows(
                SystemException.class,
                () -> resolver.resolveUrl("missing-service", "/api/items")
        );
        assertEquals(RemoteErrorCode.REMOTE_CALL_PARAM_ERROR, error.getErrorCode());
    }

    @Test
    void configuredEndpointResolverShouldUseServiceEndpointRegistry() {
        ConfiguredServiceEndpointResolver resolver = new ConfiguredServiceEndpointResolver(
                serviceId -> Optional.of(new ServiceEndpoint(serviceId, "http://gateway.local/" + serviceId, "gateway"))
        );

        assertEquals("http://gateway.local/inventory/api/items", resolver.resolveUrl("inventory", "/api/items"));
    }

    @Test
    void configuredServiceEndpointRegistryShouldReadConfiguredBaseUrls() {
        ConfiguredServiceEndpointRegistry registry = new ConfiguredServiceEndpointRegistry(properties());

        ServiceEndpoint endpoint = registry.findEndpoint("inventory").orElseThrow();

        assertEquals("inventory", endpoint.serviceId());
        assertEquals("http://inventory.local", endpoint.baseUrl());
        assertEquals("configuration", endpoint.source());
        assertTrue(registry.findEndpoint("missing-service").isEmpty());
    }

    @Test
    void configuredRemoteCallPolicyShouldStayNoopWhenDisabled() {
        RemoteHttpClientProperties properties = properties();
        properties.getPolicy().getBlockedServiceIds().add("inventory");
        ConfiguredRemoteCallPolicy policy = new ConfiguredRemoteCallPolicy(properties);

        assertDoesNotThrow(() -> policy.beforeCall(policyContext("inventory", "infra-dev-scaffolding-test")));
    }

    @Test
    void configuredRemoteCallPolicyShouldRejectBlockedService() {
        RemoteHttpClientProperties properties = properties();
        properties.getPolicy().setEnabled(true);
        properties.getPolicy().getBlockedServiceIds().add("inventory");
        ConfiguredRemoteCallPolicy policy = new ConfiguredRemoteCallPolicy(properties);

        SystemException error = assertThrows(
                SystemException.class,
                () -> policy.beforeCall(policyContext("inventory", "infra-dev-scaffolding-test"))
        );

        assertEquals(RemoteErrorCode.REMOTE_CALL_PERMISSION_DENIED, error.getErrorCode());
    }

    @Test
    void configuredRemoteCallPolicyShouldEnforceGlobalCallerAllowList() {
        RemoteHttpClientProperties properties = properties();
        properties.getPolicy().setEnabled(true);
        properties.getPolicy().getAllowedCallerIds().add("gateway-caller");
        ConfiguredRemoteCallPolicy policy = new ConfiguredRemoteCallPolicy(properties);

        assertDoesNotThrow(() -> policy.beforeCall(policyContext("inventory", "gateway-caller")));

        SystemException error = assertThrows(
                SystemException.class,
                () -> policy.beforeCall(policyContext("inventory", "infra-dev-scaffolding-test"))
        );
        assertEquals(RemoteErrorCode.REMOTE_CALL_PERMISSION_DENIED, error.getErrorCode());
    }

    @Test
    void configuredRemoteCallPolicyShouldEnforceServiceCallerAllowList() {
        RemoteHttpClientProperties properties = properties();
        properties.getPolicy().setEnabled(true);
        properties.getPolicy().getAllowedCallerIdsByService().put(
                "inventory",
                java.util.List.of("gateway-caller")
        );
        ConfiguredRemoteCallPolicy policy = new ConfiguredRemoteCallPolicy(properties);

        assertDoesNotThrow(() -> policy.beforeCall(policyContext("inventory", "gateway-caller")));

        SystemException error = assertThrows(
                SystemException.class,
                () -> policy.beforeCall(policyContext("inventory", "infra-dev-scaffolding-test"))
        );
        assertEquals(RemoteErrorCode.REMOTE_CALL_PERMISSION_DENIED, error.getErrorCode());
    }

    private RemoteCallPolicyContext policyContext(String serviceId, String callerId) {
        return new RemoteCallPolicyContext(
                "GET",
                serviceId,
                serviceId,
                "/api/items",
                "http://inventory.local/api/items",
                callerId
        );
    }

    private ConfiguredServiceEndpointResolver endpointResolver(RemoteHttpClientProperties properties) {
        return new ConfiguredServiceEndpointResolver(new ConfiguredServiceEndpointRegistry(properties));
    }

    private RemoteHttpClientProperties properties() {
        RemoteHttpClientProperties properties = new RemoteHttpClientProperties();
        properties.getServiceBaseUrls().put("inventory", "http://inventory.local");
        properties.setDefaultCallerId("infra-dev-scaffolding-test");
        return properties;
    }

    static class ItemView {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    static class RecordingRemoteCallPolicy implements RemoteCallPolicy {
        private int beforeCount;
        private int successCount;
        private int failureCount;
        private RemoteCallPolicyContext context;

        @Override
        public void beforeCall(RemoteCallPolicyContext context) {
            this.beforeCount++;
            this.context = context;
        }

        @Override
        public void afterSuccess(RemoteCallPolicyContext context) {
            this.successCount++;
            this.context = context;
        }

        @Override
        public void afterFailure(RemoteCallPolicyContext context, RuntimeException exception) {
            this.failureCount++;
            this.context = context;
        }
    }

    static class RecordingRemoteCallObserver implements RemoteCallObserver {
        private int completeCount;
        private RemoteCallObservation observation;

        @Override
        public void onComplete(RemoteCallObservation observation) {
            this.completeCount++;
            this.observation = observation;
        }
    }
}
