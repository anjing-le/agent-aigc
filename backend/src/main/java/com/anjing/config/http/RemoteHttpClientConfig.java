package com.anjing.config.http;

import com.anjing.client.ConfiguredRemoteCallPolicy;
import com.anjing.client.NoopRemoteCallObserver;
import com.anjing.client.DefaultRemoteCallerResolver;
import com.anjing.client.ConfiguredServiceEndpointRegistry;
import com.anjing.client.RemoteCallObserver;
import com.anjing.client.RemoteCallPolicy;
import com.anjing.client.RemoteCallerResolver;
import com.anjing.client.ServiceEndpointRegistry;
import com.anjing.config.properties.RemoteHttpClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP client configuration for outbound service calls.
 */
@Configuration
public class RemoteHttpClientConfig {

    @Bean
    public RestClient remoteRestClient(RemoteHttpClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(RemoteCallPolicy.class)
    public RemoteCallPolicy remoteCallPolicy(RemoteHttpClientProperties properties) {
        return new ConfiguredRemoteCallPolicy(properties);
    }

    @Bean
    @ConditionalOnMissingBean(RemoteCallObserver.class)
    public RemoteCallObserver remoteCallObserver() {
        return new NoopRemoteCallObserver();
    }

    @Bean
    @ConditionalOnMissingBean(RemoteCallerResolver.class)
    public RemoteCallerResolver remoteCallerResolver(RemoteHttpClientProperties properties) {
        return new DefaultRemoteCallerResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ServiceEndpointRegistry.class)
    public ServiceEndpointRegistry serviceEndpointRegistry(RemoteHttpClientProperties properties) {
        return new ConfiguredServiceEndpointRegistry(properties);
    }
}
