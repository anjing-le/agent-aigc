package com.anjing.client;

import com.anjing.config.properties.RemoteHttpClientProperties;
import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.errorcode.RemoteErrorCode;
import com.anjing.model.exception.SystemException;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.util.RemoteCallWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Central outbound HTTP adapter.
 *
 * <p>It keeps service-to-service headers, timeout defaults, retry behavior, and
 * audit logs consistent before the scaffold evolves into multiple services.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RemoteHttpClient {

    private final RestClient remoteRestClient;
    private final RemoteHttpClientProperties properties;
    private final ServiceEndpointResolver serviceEndpointResolver;
    private final RemoteCallerResolver remoteCallerResolver;
    private final RemoteCallPolicy remoteCallPolicy;
    private final RemoteCallObserver remoteCallObserver;

    public <R> R get(String url, Class<R> responseType) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.GET)
                .url(url)
                .build(), responseType);
    }

    public <R> R get(String url, ParameterizedTypeReference<R> responseType) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.GET)
                .url(url)
                .build(), responseType);
    }

    public <T, R> R post(String url, T body, Class<R> responseType) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.POST)
                .url(url)
                .body(body)
                .build(), responseType);
    }

    public <T, R> R post(String url, T body, ParameterizedTypeReference<R> responseType) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.POST)
                .url(url)
                .body(body)
                .build(), responseType);
    }

    public <R> R getFromService(String serviceId, String path, Class<R> responseType) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.GET)
                .serviceId(serviceId)
                .path(path)
                .build(), responseType);
    }

    public <R> R getFromService(String serviceId, String path, ParameterizedTypeReference<R> responseType) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.GET)
                .serviceId(serviceId)
                .path(path)
                .build(), responseType);
    }

    public <T, R> R postToService(String serviceId, String path, T body, Class<R> responseType) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.POST)
                .serviceId(serviceId)
                .path(path)
                .body(body)
                .build(), responseType);
    }

    public <T, R> R postToService(
            String serviceId,
            String path,
            T body,
            ParameterizedTypeReference<R> responseType
    ) {
        return exchange(RemoteHttpRequest.builder()
                .method(HttpMethod.POST)
                .serviceId(serviceId)
                .path(path)
                .body(body)
                .build(), responseType);
    }

    public <R> R exchange(RemoteHttpRequest request, Class<R> responseType) {
        validateRequest(request, responseType);
        RemoteCallPolicyContext policyContext = buildPolicyContext(request);
        long startedAtNanos = System.nanoTime();

        try {
            remoteCallPolicy.beforeCall(policyContext);
            Map<String, Object> descriptor = describeRequest(request);
            R response = RemoteCallWrapper.callWithRetry(
                    ignored -> doExchange(request, responseType, policyContext),
                    descriptor,
                    remoteCallName(request),
                    resolveRetryCount(request),
                    request.isCheckResponse(),
                    resolveRetryInterval(request)
            );
            observeRemoteCall(policyContext, true, startedAtNanos, null);
            return response;
        } catch (RuntimeException e) {
            observeRemoteCall(policyContext, false, startedAtNanos, e);
            throw e;
        }
    }

    public <R> R exchange(RemoteHttpRequest request, ParameterizedTypeReference<R> responseType) {
        validateRequest(request, responseType);
        RemoteCallPolicyContext policyContext = buildPolicyContext(request);
        long startedAtNanos = System.nanoTime();

        try {
            remoteCallPolicy.beforeCall(policyContext);
            Map<String, Object> descriptor = describeRequest(request);
            R response = RemoteCallWrapper.callWithRetry(
                    ignored -> doExchange(request, responseType, policyContext),
                    descriptor,
                    remoteCallName(request),
                    resolveRetryCount(request),
                    request.isCheckResponse(),
                    resolveRetryInterval(request)
            );
            observeRemoteCall(policyContext, true, startedAtNanos, null);
            return response;
        } catch (RuntimeException e) {
            observeRemoteCall(policyContext, false, startedAtNanos, e);
            throw e;
        }
    }

    private <R> R doExchange(
            RemoteHttpRequest request,
            Class<R> responseType,
            RemoteCallPolicyContext policyContext
    ) {
        return doExchange(request, responseSpec -> responseSpec.body(responseType), policyContext);
    }

    private <R> R doExchange(
            RemoteHttpRequest request,
            ParameterizedTypeReference<R> responseType,
            RemoteCallPolicyContext policyContext
    ) {
        return doExchange(request, responseSpec -> responseSpec.body(responseType), policyContext);
    }

    private <R> R doExchange(
            RemoteHttpRequest request,
            Function<RestClient.ResponseSpec, R> responseReader,
            RemoteCallPolicyContext policyContext
    ) {
        try {
            String url = resolveUrl(request);
            RestClient.RequestBodySpec spec = remoteRestClient
                    .method(resolveMethod(request))
                    .uri(url);

            buildHeaders(request).forEach((name, value) -> spec.header(name, value));

            RestClient.ResponseSpec responseSpec = shouldSendBody(request)
                    ? spec.body(request.getBody()).retrieve()
                    : spec.retrieve();

            R response = responseReader.apply(responseSpec);
            remoteCallPolicy.afterSuccess(policyContext);
            return response;
        } catch (ResourceAccessException e) {
            throw recordFailure(policyContext, new SystemException(
                    "远程 HTTP 调用网络异常: " + remoteCallName(request),
                    e,
                    RemoteErrorCode.REMOTE_CALL_NETWORK_ERROR
            ));
        } catch (RestClientResponseException e) {
            throw recordFailure(policyContext, new SystemException(
                    String.format("远程 HTTP 响应异常: %s, status=%s", remoteCallName(request), e.getStatusCode().value()),
                    e,
                    remoteErrorCode(e.getStatusCode())
            ));
        } catch (RestClientException e) {
            throw recordFailure(policyContext, new SystemException(
                    "远程 HTTP 调用失败: " + remoteCallName(request),
                    e,
                    RemoteErrorCode.REMOTE_CALL_FAILED
            ));
        } catch (RuntimeException e) {
            throw recordFailure(policyContext, e);
        }
    }

    private Map<String, String> buildHeaders(RemoteHttpRequest request) {
        Map<String, String> headers = RemoteCallWrapper.serviceCallHeaders(resolveCallerId(request));
        if (request.getHeaders() == null) {
            return headers;
        }

        request.getHeaders().forEach((name, value) -> {
            if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
                headers.put(name, value);
            }
        });
        return headers;
    }

    private Map<String, Object> describeRequest(RemoteHttpRequest request) {
        Map<String, Object> descriptor = new LinkedHashMap<>();
        descriptor.put("method", resolveMethod(request).name());
        descriptor.put("targetService", resolveTargetService(request));
        descriptor.put("url", sanitizedUrl(resolveUrl(request)));
        descriptor.put("callerId", resolveCallerId(request));
        return descriptor;
    }

    private RemoteCallPolicyContext buildPolicyContext(RemoteHttpRequest request) {
        String url = resolveUrl(request);
        return new RemoteCallPolicyContext(
                resolveMethod(request).name(),
                resolveTargetService(request),
                request.getServiceId(),
                resolvePolicyPath(request, url),
                sanitizedUrl(url),
                resolveCallerId(request)
        );
    }

    private String resolvePolicyPath(RemoteHttpRequest request, String url) {
        if (StringUtils.hasText(request.getPath())) {
            return request.getPath();
        }

        try {
            return URI.create(url).getPath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private <E extends RuntimeException> E recordFailure(RemoteCallPolicyContext context, E exception) {
        remoteCallPolicy.afterFailure(context, exception);
        return exception;
    }

    private void observeRemoteCall(
            RemoteCallPolicyContext context,
            boolean success,
            long startedAtNanos,
            RuntimeException exception
    ) {
        safeObserve(new RemoteCallObservation(
                context.method(),
                context.targetService(),
                context.serviceId(),
                context.path(),
                context.url(),
                context.callerId(),
                contextValue(GlobalRequestContext::getRequestId),
                contextValue(GlobalRequestContext::getTraceId),
                contextValue(GlobalRequestContext::getTenantId),
                contextValue(GlobalRequestContext::getUserId),
                contextValue(GlobalRequestContext::getTimeZone),
                contextValue(GlobalRequestContext::getLocale),
                success,
                durationMs(startedAtNanos),
                errorCode(exception),
                exception == null ? null : exception.getMessage(),
                exception == null ? null : exception.getClass().getSimpleName()
        ));
    }

    private void safeObserve(RemoteCallObservation observation) {
        try {
            remoteCallObserver.onComplete(observation);
        } catch (RuntimeException e) {
            log.warn("Remote call observer failed: {}", e.getMessage(), e);
        }
    }

    private long durationMs(long startedAtNanos) {
        return Math.max(0L, (System.nanoTime() - startedAtNanos) / 1_000_000L);
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof SystemException systemException && systemException.getErrorCode() != null) {
            return systemException.getErrorCode().getCode();
        }
        if (exception != null && exception.getCause() instanceof SystemException cause
                && cause.getErrorCode() != null) {
            return cause.getErrorCode().getCode();
        }
        return null;
    }

    private String contextValue(Function<GlobalRequestContext, String> getter) {
        return GlobalRequestContextHolder.current().map(getter).orElse(null);
    }

    private void validateRequest(RemoteHttpRequest request, Object responseType) {
        if (request == null) {
            throw new SystemException("远程 HTTP 请求不能为空", RemoteErrorCode.REMOTE_CALL_PARAM_ERROR);
        }
        validateAbsoluteUrl(resolveUrl(request));
        if (responseType == null) {
            throw new SystemException("远程 HTTP 响应类型不能为空", RemoteErrorCode.REMOTE_CALL_PARAM_ERROR);
        }
    }

    private void validateAbsoluteUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getAuthority())) {
                throw new IllegalArgumentException("Remote HTTP URL must be absolute");
            }
        } catch (Exception e) {
            throw new SystemException("远程 HTTP URL 必须是绝对地址", e, RemoteErrorCode.REMOTE_CALL_PARAM_ERROR);
        }
    }

    private HttpMethod resolveMethod(RemoteHttpRequest request) {
        return request.getMethod() == null ? HttpMethod.GET : request.getMethod();
    }

    private String resolveCallerId(RemoteHttpRequest request) {
        return remoteCallerResolver.resolveCallerId(request);
    }

    private int resolveRetryCount(RemoteHttpRequest request) {
        return request.getRetryCount() == null ? properties.getDefaultRetryCount() : request.getRetryCount();
    }

    private long resolveRetryInterval(RemoteHttpRequest request) {
        return request.getRetryIntervalMs() == null
                ? properties.getDefaultRetryIntervalMs()
                : request.getRetryIntervalMs();
    }

    private boolean shouldSendBody(RemoteHttpRequest request) {
        return request.getBody() != null
                && (HttpMethod.POST.equals(resolveMethod(request))
                || HttpMethod.PUT.equals(resolveMethod(request))
                || HttpMethod.PATCH.equals(resolveMethod(request)));
    }

    private String remoteCallName(RemoteHttpRequest request) {
        return resolveMethod(request).name() + " " + resolveTargetService(request);
    }

    private String resolveTargetService(RemoteHttpRequest request) {
        if (StringUtils.hasText(request.getTargetService())) {
            return request.getTargetService();
        }
        if (StringUtils.hasText(request.getServiceId())) {
            return request.getServiceId();
        }

        try {
            URI uri = URI.create(resolveUrl(request));
            return StringUtils.hasText(uri.getHost()) ? uri.getHost() : "unknown-service";
        } catch (Exception ignored) {
            return "unknown-service";
        }
    }

    private String resolveUrl(RemoteHttpRequest request) {
        if (StringUtils.hasText(request.getUrl())) {
            return request.getUrl();
        }
        if (!StringUtils.hasText(request.getServiceId())) {
            throw new SystemException("远程 HTTP URL 或 serviceId 不能为空", RemoteErrorCode.REMOTE_CALL_PARAM_ERROR);
        }

        return serviceEndpointResolver.resolveUrl(request.getServiceId(), request.getPath());
    }

    private String sanitizedUrl(String url) {
        try {
            URI uri = URI.create(url);
            String queryMarker = StringUtils.hasText(uri.getQuery()) ? "?..." : "";
            return URI.create(uri.getScheme() + "://" + uri.getAuthority() + uri.getPath()).toString() + queryMarker;
        } catch (Exception ignored) {
            return url;
        }
    }

    private RemoteErrorCode remoteErrorCode(HttpStatusCode statusCode) {
        int status = statusCode.value();
        if (status == 408 || status == 504) {
            return RemoteErrorCode.REMOTE_CALL_TIMEOUT;
        }
        if (status == 429 || status == 502 || status == 503) {
            return RemoteErrorCode.REMOTE_CALL_NETWORK_ERROR;
        }
        return RemoteErrorCode.REMOTE_RESPONSE_STATUS_FAILED;
    }
}
