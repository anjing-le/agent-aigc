package com.anjing.util;

import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.constants.PlatformContractConstants;
import com.anjing.model.constants.RequestHeaderConstants;
import com.anjing.model.exception.SystemException;
import com.anjing.model.errorcode.RemoteErrorCode;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.model.response.APIResponse;
import com.anjing.model.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 🌐 远程调用包装工具类 - 统一RPC/HTTP调用的日志和异常处理
 * 
 * <h3>🚀 核心功能：</h3>
 * <ul>
 *   <li>📋 <b>统一日志记录</b> - 自动记录请求、响应、耗时信息</li>
 *   <li>🔍 <b>响应结果校验</b> - 支持APIResponse、BaseResponse等多种格式</li>
 *   <li>🛡️ <b>异常统一处理</b> - 统一转换为SystemException</li>
 *   <li>⏱️ <b>性能监控</b> - 自动记录调用耗时</li>
 *   <li>🔄 <b>重试机制</b> - 支持失败重试</li>
 * </ul>
 * 
 * <h3>🎯 适用场景：</h3>
 * <ul>
 *   <li>🌐 <b>Dubbo RPC调用</b> - 微服务间的RPC调用</li>
 *   <li>🌍 <b>HTTP接口调用</b> - 第三方API调用</li>
 *   <li>🔗 <b>内部服务调用</b> - 模块间的服务调用</li>
 *   <li>📡 <b>外部系统集成</b> - 与外部系统的接口调用</li>
 * </ul>
 * 
 * <h3>📝 基础用法：</h3>
 * <pre>
 * // 最简单的用法
 * OrderResponse response = RemoteCallWrapper.call(
 *     req -> orderService.createOrder(req), 
 *     orderRequest
 * );
 * 
 * // 指定调用名称
 * UserResponse response = RemoteCallWrapper.call(
 *     req -> userService.getUser(req), 
 *     userRequest,
 *     "getUserInfo"
 * );
 * </pre>
 * 
 * <h3>🔧 高级用法：</h3>
 * <pre>
 * // 带重试和校验
 * PaymentResponse response = RemoteCallWrapper.callWithRetry(
 *     req -> paymentService.processPayment(req),
 *     paymentRequest,
 *     "processPayment",
 *     3,  // 重试3次
 *     true, // 检查响应状态
 *     1000  // 重试间隔1秒
 * );
 * 
 * // 无参数调用
 * ConfigResponse config = RemoteCallWrapper.callNoParam(
 *     () -> configService.getConfig(),
 *     "getSystemConfig"
 * );
 *
 * // HTTP/WebClient/Feign 调用前准备上下文请求头
 * Map&lt;String, String&gt; headers = RemoteCallWrapper.serviceCallHeaders(ServiceBoundaryConstants.APPLICATION_ID);
 * </pre>
 * 
 * <h3>📊 自动日志输出：</h3>
 * <pre>
 * 🚀 [RemoteCall] 开始调用: createOrder
 * 📝 [RemoteCall] 请求参数: {"orderId":"12345","amount":100.00}
 * ✅ [RemoteCall] 调用成功: createOrder | 耗时: 156ms
 * 📝 [RemoteCall] 响应结果: {"code":"0","message":"success","data":{"orderId":"12345"}}
 * </pre>
 * 
 * <h3>🛡️ 支持的响应格式：</h3>
 * <ul>
 *   <li><b>APIResponse&lt;T&gt;</b> - 项目标准响应格式</li>
 *   <li><b>BaseResponse</b> - 通用响应基类</li>
 *   <li><b>自定义响应</b> - 包含success、isSuccess()等字段的响应</li>
 * </ul>
 * 
 * @author Backend Template Team
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
public class RemoteCallWrapper {

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_RETRY_COUNT = 0;
    
    /**
     * 默认重试间隔（毫秒）
     */
    private static final long DEFAULT_RETRY_INTERVAL = 1000L;

    /**
     * Builds outbound headers from the current request context.
     *
     * <p>Use this when an HTTP client, RPC filter, or gateway adapter needs to
     * forward request identity, trace identity, tenant, user, locale, and time
     * zone to the next service.</p>
     *
     * @return headers collected from {@link GlobalRequestContextHolder}; empty when no context exists
     */
    public static Map<String, String> currentContextHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        GlobalRequestContextHolder.current().ifPresent(context -> appendContextHeaders(headers, context));
        return headers;
    }

    /**
     * Builds service-to-service headers and guarantees a request/trace id pair.
     *
     * <p>When the call starts from a scheduler, async job, or other non-web
     * entrypoint, there may be no inbound request context. In that case this
     * method creates a root request id so downstream logs can still be traced.</p>
     *
     * @param callerId current application or service id
     * @return outbound headers for a remote service call
     */
    public static Map<String, String> serviceCallHeaders(String callerId) {
        Map<String, String> headers = currentContextHeaders();
        putIfHasText(headers, RequestHeaderConstants.CALLER_ID, callerId);
        ensureRequestTraceHeaders(headers);
        return headers;
    }

    /**
     * 🚀 基础远程调用 - 最简单的用法
     * 
     * @param function 调用函数
     * @param request  请求参数
     * @param <T>      请求类型
     * @param <R>      响应类型
     * @return 响应结果
     */
    public static <T, R> R call(Function<T, R> function, T request) {
        return call(function, request, getRequestName(request), true);
    }

    /**
     * 🎯 指定调用名称的远程调用
     * 
     * @param function    调用函数
     * @param request     请求参数
     * @param methodName  方法名称
     * @param <T>         请求类型
     * @param <R>         响应类型
     * @return 响应结果
     */
    public static <T, R> R call(Function<T, R> function, T request, String methodName) {
        return call(function, request, methodName, true);
    }

    /**
     * 🔍 可控制校验的远程调用
     * 
     * @param function      调用函数
     * @param request       请求参数
     * @param methodName    方法名称
     * @param checkResponse 是否校验响应
     * @param <T>           请求类型
     * @param <R>           响应类型
     * @return 响应结果
     */
    public static <T, R> R call(Function<T, R> function, T request, String methodName, boolean checkResponse) {
        return callWithRetry(function, request, methodName, DEFAULT_RETRY_COUNT, checkResponse, DEFAULT_RETRY_INTERVAL);
    }

    /**
     * 🔄 带重试机制的远程调用
     * 
     * @param function       调用函数
     * @param request        请求参数
     * @param methodName     方法名称
     * @param retryCount     重试次数
     * @param checkResponse  是否校验响应
     * @param retryInterval  重试间隔（毫秒）
     * @param <T>            请求类型
     * @param <R>            响应类型
     * @return 响应结果
     */
    public static <T, R> R callWithRetry(Function<T, R> function, T request, String methodName, 
                                        int retryCount, boolean checkResponse, long retryInterval) {
        StopWatch stopWatch = new StopWatch();
        R response = null;
        Exception lastException = null;
        
        // 记录调用开始
        logCallStart(methodName, request);
        
        // 执行调用（包含重试逻辑）
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                stopWatch.start();
                response = function.apply(request);
                stopWatch.stop();
                
                // 校验响应
                if (checkResponse) {
                    validateResponse(response, methodName);
                }
                
                // 记录成功日志
                logCallSuccess(methodName, request, response, stopWatch.getTotalTimeMillis(), attempt);
                return response;
                
            } catch (Exception e) {
                if (stopWatch.isRunning()) {
                    stopWatch.stop();
                }
                
                lastException = e;
                
                // 如果是最后一次尝试，或者不需要重试的异常，直接抛出
                if (attempt >= retryCount || !shouldRetry(e)) {
                    break;
                }
                
                // 记录重试日志
                logRetryAttempt(methodName, attempt + 1, retryCount, e.getMessage());
                
                // 等待重试间隔
                if (retryInterval > 0) {
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    throw new SystemException("远程调用被中断", RemoteErrorCode.REMOTE_CALL_FAILED);
                    }
                }
            }
        }
        
        // 所有重试都失败了
        logCallFailure(methodName, request, lastException, stopWatch.getTotalTimeMillis());
        throw new SystemException("远程调用失败: " + lastException.getMessage(), lastException, RemoteErrorCode.REMOTE_CALL_FAILED);
    }

    /**
     * 📞 无参数远程调用
     * 
     * @param supplier   调用供应商
     * @param methodName 方法名称
     * @param <R>        响应类型
     * @return 响应结果
     */
    public static <R> R callNoParam(Supplier<R> supplier, String methodName) {
        return callNoParamWithRetry(supplier, methodName, DEFAULT_RETRY_COUNT, true, DEFAULT_RETRY_INTERVAL);
    }

    /**
     * 🔄 无参数带重试的远程调用
     * 
     * @param supplier       调用供应商
     * @param methodName     方法名称
     * @param retryCount     重试次数
     * @param checkResponse  是否校验响应
     * @param retryInterval  重试间隔（毫秒）
     * @param <R>            响应类型
     * @return 响应结果
     */
    public static <R> R callNoParamWithRetry(Supplier<R> supplier, String methodName, 
                                           int retryCount, boolean checkResponse, long retryInterval) {
        return callWithRetry(
            (Void v) -> supplier.get(), 
            null, 
            methodName, 
            retryCount, 
            checkResponse, 
            retryInterval
        );
    }

    /**
     * 🔍 校验响应结果
     */
    private static <R> void validateResponse(R response, String methodName) {
        if (response == null) {
            throw new SystemException("远程调用响应为空: " + methodName, RemoteErrorCode.REMOTE_RESPONSE_NULL);
        }

        // 校验APIResponse格式
        if (response instanceof APIResponse) {
            APIResponse<?> apiResponse = (APIResponse<?>) response;
            if (!apiResponse.isSuccess()) {
                throw new SystemException(String.format("远程调用响应失败: %s, code: %s, message: %s", 
                                methodName, apiResponse.getCode(), apiResponse.getMessage()), RemoteErrorCode.REMOTE_RESPONSE_STATUS_FAILED);
            }
            return;
        }

        // 校验BaseResponse格式
        if (response instanceof BaseResponse) {
            BaseResponse baseResponse = (BaseResponse) response;
            if (baseResponse.getSuccess() == null || !baseResponse.getSuccess()) {
                throw new SystemException(String.format("远程调用响应失败: %s, code: %s, message: %s", 
                                methodName, baseResponse.getResponseCode(), baseResponse.getResponseMessage()), RemoteErrorCode.REMOTE_RESPONSE_STATUS_FAILED);
            }
            return;
        }

        // 通过反射校验其他格式的响应
        if (!validateByReflection(response)) {
            throw new SystemException("远程调用响应状态校验失败: " + methodName, RemoteErrorCode.REMOTE_RESPONSE_STATUS_FAILED);
        }
    }

    /**
     * 🔍 通过反射校验响应状态
     */
    private static <R> boolean validateByReflection(R response) {
        try {
            Class<?> responseClass = response.getClass();
            
            // 尝试常见的成功状态方法
            String[] successMethods = {"isSuccess", "isSucceeded", "getSuccess"};
            for (String methodName : successMethods) {
                try {
                    Method method = responseClass.getMethod(methodName);
                    Object result = method.invoke(response);
                    if (result instanceof Boolean) {
                        return (Boolean) result;
                    }
                } catch (Exception ignored) {
                    // 方法不存在或调用失败，继续尝试下一个
                }
            }
            
            // 如果没有找到成功状态方法，默认认为成功
            return true;
            
        } catch (Exception e) {
            log.warn("反射校验响应状态失败: {}", e.getMessage());
            return true; // 默认认为成功
        }
    }

    /**
     * 🤔 判断是否应该重试
     */
    private static boolean shouldRetry(Exception e) {
        // 对于业务异常，通常不重试
        if (e instanceof SystemException) {
            SystemException se = (SystemException) e;
            String errorCode = se.getErrorCode().getCode();
            return isRetryableErrorCode(errorCode);
        }
        
        // 网络异常、超时异常等可以重试
        return e instanceof java.net.SocketTimeoutException ||
               e instanceof java.net.ConnectException ||
               e instanceof java.net.SocketException ||
               e instanceof java.io.IOException;
    }

    private static boolean isRetryableErrorCode(String errorCode) {
        for (String range : PlatformContractConstants.ErrorCodes.RETRYABLE_RANGES) {
            if (isCodeInRange(errorCode, range)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCodeInRange(String errorCode, String range) {
        if (!StringUtils.hasText(errorCode) || !errorCode.matches("\\d+")) {
            return false;
        }

        int code = Integer.parseInt(errorCode);
        String[] parts = range.split("-");
        if (parts.length == 1) {
            return code == Integer.parseInt(parts[0]);
        }

        if (parts.length != 2) {
            return false;
        }

        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);
        return code >= start && code <= end;
    }

    /**
     * 📝 记录调用开始日志
     */
    private static <T> void logCallStart(String methodName, T request) {
        try {
            if (request != null) {
                String requestJson = JsonUtils.toJson(request);
                log.info("🚀 [RemoteCall] 开始调用: {} | 请求参数: {}", methodName, requestJson);
            } else {
                log.info("🚀 [RemoteCall] 开始调用: {} | 无参数", methodName);
            }
        } catch (Exception e) {
            log.info("🚀 [RemoteCall] 开始调用: {} | 请求参数: [序列化失败: {}]", methodName, e.getMessage());
        }
    }

    /**
     * ✅ 记录调用成功日志
     */
    private static <T, R> void logCallSuccess(String methodName, T request, R response, long timeMillis, int attempt) {
        try {
            String responseJson = JsonUtils.toJson(response);
            String retryInfo = attempt > 0 ? String.format(" | 重试: %d次", attempt) : "";
            log.info("✅ [RemoteCall] 调用成功: {} | 耗时: {}ms{} | 响应结果: {}", 
                    methodName, timeMillis, retryInfo, truncateString(responseJson, 500));
        } catch (Exception e) {
            log.info("✅ [RemoteCall] 调用成功: {} | 耗时: {}ms | 响应结果: [序列化失败: {}]", 
                    methodName, timeMillis, e.getMessage());
        }
    }

    /**
     * ❌ 记录调用失败日志
     */
    private static <T> void logCallFailure(String methodName, T request, Exception e, long timeMillis) {
        try {
            String requestJson = request != null ? JsonUtils.toJson(request) : "无参数";
            log.error("❌ [RemoteCall] 调用失败: {} | 耗时: {}ms | 请求参数: {} | 异常: {}", 
                    methodName, timeMillis, requestJson, e.getMessage());
        } catch (Exception ex) {
            log.error("❌ [RemoteCall] 调用失败: {} | 耗时: {}ms | 请求参数: [序列化失败] | 异常: {}", 
                    methodName, timeMillis, e.getMessage());
        }
    }

    /**
     * 🔄 记录重试日志
     */
    private static void logRetryAttempt(String methodName, int currentAttempt, int maxRetry, String errorMessage) {
        log.warn("🔄 [RemoteCall] 重试调用: {} | 第{}次重试 (最多{}次) | 原因: {}", 
                methodName, currentAttempt, maxRetry, errorMessage);
    }

    /**
     * Adds all non-empty request context fields to outbound headers.
     */
    private static void appendContextHeaders(Map<String, String> headers, GlobalRequestContext context) {
        for (String key : PlatformContractConstants.BACKEND_PROPAGATED_HEADER_KEYS) {
            appendContextHeader(headers, context, key);
        }
    }

    private static void appendContextHeader(Map<String, String> headers, GlobalRequestContext context, String key) {
        switch (key) {
            case "requestId" -> putIfHasText(headers, RequestHeaderConstants.REQUEST_ID, context.getRequestId());
            case "traceId" -> putIfHasText(headers, RequestHeaderConstants.TRACE_ID, context.getTraceId());
            case "tenantId" -> putIfHasText(headers, RequestHeaderConstants.TENANT_ID, context.getTenantId());
            case "userId" -> putIfHasText(headers, RequestHeaderConstants.USER_ID, context.getUserId());
            case "userName" -> putIfHasText(headers, RequestHeaderConstants.USER_NAME, context.getUserName());
            case "userRoles" -> putIfHasText(headers, RequestHeaderConstants.USER_ROLES, context.getUserRoles());
            case "callerId" -> putIfHasText(headers, RequestHeaderConstants.CALLER_ID, context.getCallerId());
            case "timeZone" -> putIfHasText(headers, RequestHeaderConstants.TIME_ZONE, context.getTimeZone());
            case "acceptLanguage" -> putIfHasText(headers, RequestHeaderConstants.ACCEPT_LANGUAGE, context.getLocale());
            default -> {
                // Unknown keys are ignored so generated contract changes must be guarded by tests and scripts.
            }
        }
    }

    private static void ensureRequestTraceHeaders(Map<String, String> headers) {
        String requestId = headers.get(RequestHeaderConstants.REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
            headers.put(RequestHeaderConstants.REQUEST_ID, requestId);
        }

        if (!StringUtils.hasText(headers.get(RequestHeaderConstants.TRACE_ID))) {
            headers.put(RequestHeaderConstants.TRACE_ID, requestId);
        }
    }

    private static void putIfHasText(Map<String, String> headers, String name, String value) {
        if (StringUtils.hasText(value)) {
            headers.put(name, value);
        }
    }

    /**
     * 📝 获取请求名称
     */
    private static <T> String getRequestName(T request) {
        if (request == null) {
            return "UnknownRequest";
        }
        return request.getClass().getSimpleName();
    }

    /**
     * ✂️ 截断字符串（避免日志过长）
     */
    private static String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }
}
