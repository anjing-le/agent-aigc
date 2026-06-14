package com.anjing.aspect;

import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.errorcode.ErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.model.exception.SystemException;
import com.anjing.model.response.APIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 统一日志处理切面
 * 
 * 功能：
 * 1. 记录Controller接口的入参、出参
 * 2. 记录接口执行时间
 * 3. 记录请求信息（IP、URL、方法等）
 * 4. 异常日志处理
 * 
 * 通过AOP面向切面编程，零侵入完成对接口信息的监控
 * 
 * @author Backend Template
 * @since 2025-01-20
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ControllerLogAspect
{

    private static final String FILTERED_VALUE = "***FILTERED***";
    private static final Set<String> SENSITIVE_FIELD_NAMES = Set.of(
            "password",
            "credential",
            "apiKey",
            "accessKey",
            "secretKey",
            "accessKeySecret",
            "token",
            "refreshToken",
            "authorization"
    );

    private final ObjectMapper objectMapper;

    /**
     * 线程本地变量存储请求上下文
     */
    private final ThreadLocal<RequestContext> requestContextHolder = new ThreadLocal<>();

    /**
     * 定义切点：Controller包下的所有方法
     */
    @Pointcut("execution(public * com.anjing.controller..*.*(..)) || execution(public * com.anjing.aigc.controller..*.*(..))")
    public void controllerMethod() {}

    /**
     * 环绕通知：记录执行时间和返回结果
     */
    @Around("controllerMethod()")
    public Object aroundMethod(ProceedingJoinPoint joinPoint) throws Throwable
    {
        RequestContext context = createRequestContext(joinPoint);
        if (context == null) {
            return joinPoint.proceed();
        }

        requestContextHolder.set(context);
        logRequestStart(context);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - context.getStartTime();

            log.info("API_REQUEST_END | requestId={} | path={} | httpMethod={} | controller={} | action={} | durationMs={} | errorCode={} | result={}",
                    context.getRequestId(),
                    context.getPath(),
                    context.getMethod(),
                    getSimpleClassName(context.getClassName()),
                    context.getMethodName(),
                    executionTime,
                    resolveResultCode(result),
                    formatResult(result));

            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - context.getStartTime();

            log.error("API_REQUEST_ERROR | requestId={} | path={} | httpMethod={} | controller={} | action={} | durationMs={} | errorCode={} | error={}",
                    context.getRequestId(),
                    context.getPath(),
                    context.getMethod(),
                    getSimpleClassName(context.getClassName()),
                    context.getMethodName(),
                    executionTime,
                    resolveExceptionCode(e),
                    e.getMessage(), e);

            throw e;
        } finally {
            requestContextHolder.remove();
        }
    }

    private RequestContext createRequestContext(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }

            HttpServletRequest request = attributes.getRequest();
            RequestContext context = new RequestContext();
            context.setStartTime(System.currentTimeMillis());
            context.setRequestId(resolveRequestId());
            context.setPath(request.getRequestURI());
            context.setUrl(request.getRequestURL().toString());
            context.setMethod(request.getMethod());
            context.setIp(getClientIp(request));
            context.setUserAgent(request.getHeader("User-Agent"));
            context.setClassName(joinPoint.getSignature().getDeclaringTypeName());
            context.setMethodName(joinPoint.getSignature().getName());
            context.setArgs(joinPoint.getArgs());
            return context;
        } catch (Exception e) {
            log.error("记录请求日志失败", e);
            return null;
        }
    }

    private void logRequestStart(RequestContext context) {
        log.info("API_REQUEST_START | requestId={} | path={} | httpMethod={} | controller={} | action={} | clientIp={} | args={}",
                context.getRequestId(),
                context.getPath(),
                context.getMethod(),
                getSimpleClassName(context.getClassName()),
                context.getMethodName(),
                context.getIp(),
                formatArgs(context.getArgs()));
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Resolve the request id created by RequestContextFilter.
     */
    private String resolveRequestId() {
        String requestId = GlobalRequestContextHolder.requestIdOrEmpty();
        return requestId.isEmpty() ? "unknown" : requestId;
    }

    private String resolveResultCode(Object result) {
        if (result instanceof APIResponse) {
            return normalizeCode(((APIResponse<?>) result).getCode());
        }
        return "unknown";
    }

    private String resolveExceptionCode(Throwable exception) {
        if (exception instanceof BizException) {
            return resolveErrorCode(((BizException) exception).getErrorCode());
        }
        if (exception instanceof SystemException) {
            return resolveErrorCode(((SystemException) exception).getErrorCode());
        }
        return "unknown";
    }

    private String resolveErrorCode(ErrorCode errorCode) {
        return errorCode == null ? "unknown" : normalizeCode(errorCode.getCode());
    }

    private String normalizeCode(String code) {
        return code == null || code.trim().isEmpty() ? "unknown" : code;
    }

    /**
     * 获取简单类名
     */
    private String getSimpleClassName(String fullClassName) {
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    /**
     * 格式化参数
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            // 过滤敏感信息
            Object[] filteredArgs = Arrays.stream(args)
                    .map(this::sanitizeForLog)
                    .toArray();
            
            String jsonArgs = objectMapper.writeValueAsString(filteredArgs);
            
            // 限制日志长度
            if (jsonArgs.length() > 1000) {
                return jsonArgs.substring(0, 1000) + "...";
            }
            return jsonArgs;
            
        } catch (Exception e) {
            return maskSensitiveText(Arrays.toString(args));
        }
    }

    /**
     * 格式化返回结果
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }
        
        try {
            String jsonResult = objectMapper.writeValueAsString(sanitizeForLog(result));
            
            // 限制日志长度
            if (jsonResult.length() > 1000) {
                return jsonResult.substring(0, 1000) + "...";
            }
            return jsonResult;
            
        } catch (Exception e) {
            return maskSensitiveText(result.toString());
        }
    }

    /**
     * 过滤敏感数据
     */
    private Object sanitizeForLog(Object value) {
        if (value == null) {
            return null;
        }

        try {
            JsonNode node = objectMapper.valueToTree(value);
            redactJsonNode(node);
            return node;
        } catch (Exception e) {
            return maskSensitiveText(value.toString());
        }
    }

    private void redactJsonNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (isSensitiveField(field.getKey())) {
                    objectNode.put(field.getKey(), FILTERED_VALUE);
                } else {
                    redactJsonNode(field.getValue());
                }
            }
            return;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode item : arrayNode) {
                redactJsonNode(item);
            }
        }
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        return SENSITIVE_FIELD_NAMES.stream()
                .anyMatch(sensitiveField -> sensitiveField.equalsIgnoreCase(fieldName));
    }

    private String maskSensitiveText(String text) {
        if (text == null) {
            return null;
        }
        String masked = text;
        for (String fieldName : SENSITIVE_FIELD_NAMES) {
            masked = masked.replaceAll(
                    "(?i)(" + fieldName + "\\s*[=:]\\s*)([^,}\\]\\s]+)",
                    "$1" + FILTERED_VALUE
            );
        }
        return masked;
    }

    /**
     * 请求上下文
     */
    private static class RequestContext {
        private long startTime;
        private String requestId;
        private String path;
        private String url;
        private String method;
        private String ip;
        private String userAgent;
        private String className;
        private String methodName;
        private Object[] args;

        // getters and setters
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        
        public Object[] getArgs() { return args; }
        public void setArgs(Object[] args) { this.args = args; }
    }
}
