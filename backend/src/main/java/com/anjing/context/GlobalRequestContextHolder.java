package com.anjing.context;

import com.anjing.model.request.GlobalRequestContext;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Thread-local request context for controllers, services, logs, and remote calls.
 */
public final class GlobalRequestContextHolder {

    private static final ThreadLocal<GlobalRequestContext> HOLDER = new ThreadLocal<>();

    private GlobalRequestContextHolder() {
    }

    public static void set(GlobalRequestContext context) {
        HOLDER.set(context);
    }

    public static Optional<GlobalRequestContext> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static Optional<GlobalRequestContext> capture() {
        return current().map(GlobalRequestContextHolder::copyOf);
    }

    public static GlobalRequestContext copyOf(GlobalRequestContext context) {
        if (context == null) {
            return null;
        }

        Object[] args = context.getArgs();
        return GlobalRequestContext.builder()
                .requestId(context.getRequestId())
                .traceId(context.getTraceId())
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .userName(context.getUserName())
                .userRoles(context.getUserRoles())
                .callerId(context.getCallerId())
                .locale(context.getLocale())
                .timeZone(context.getTimeZone())
                .ip(context.getIp())
                .url(context.getUrl())
                .methodType(context.getMethodType())
                .className(context.getClassName())
                .methodName(context.getMethodName())
                .args(args == null ? null : args.clone())
                .build();
    }

    public static void setOrClear(GlobalRequestContext context) {
        if (context == null) {
            clear();
            return;
        }
        set(copyOf(context));
    }

    public static void runWith(GlobalRequestContext context, Runnable runnable) {
        GlobalRequestContext previous = capture().orElse(null);
        try {
            setOrClear(context);
            runnable.run();
        } finally {
            setOrClear(previous);
        }
    }

    public static <T> T callWith(GlobalRequestContext context, Callable<T> callable) throws Exception {
        GlobalRequestContext previous = capture().orElse(null);
        try {
            setOrClear(context);
            return callable.call();
        } finally {
            setOrClear(previous);
        }
    }

    public static String requestIdOrEmpty() {
        return valueOrEmpty(GlobalRequestContext::getRequestId);
    }

    public static String requestIdOrNull() {
        return valueOrNull(GlobalRequestContext::getRequestId);
    }

    public static String traceIdOrEmpty() {
        return valueOrEmpty(GlobalRequestContext::getTraceId);
    }

    public static String traceIdOrNull() {
        return valueOrNull(GlobalRequestContext::getTraceId);
    }

    public static String tenantIdOrEmpty() {
        return valueOrEmpty(GlobalRequestContext::getTenantId);
    }

    public static String userIdOrEmpty() {
        return valueOrEmpty(GlobalRequestContext::getUserId);
    }

    public static String callerIdOrEmpty() {
        return valueOrEmpty(GlobalRequestContext::getCallerId);
    }

    public static String localeOrEmpty() {
        return valueOrEmpty(GlobalRequestContext::getLocale);
    }

    public static String timeZoneOrEmpty() {
        return valueOrEmpty(GlobalRequestContext::getTimeZone);
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static String valueOrEmpty(Function<GlobalRequestContext, String> getter) {
        String value = valueOrNull(getter);
        return value == null ? "" : value;
    }

    private static String valueOrNull(Function<GlobalRequestContext, String> getter) {
        return current().map(getter).orElse(null);
    }
}
