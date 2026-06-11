package com.anjing.config.http;

import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.constants.RequestHeaderConstants;
import com.anjing.model.request.GlobalRequestContext;
import com.anjing.util.LocaleUtils;
import com.anjing.util.TimeZoneUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Builds a request context that can survive future gateway and microservice hops.
 */
@Component("globalRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        GlobalRequestContext context = buildContext(request);

        try {
            GlobalRequestContextHolder.set(context);
            applyMdc(context);
            response.setHeader(RequestHeaderConstants.REQUEST_ID, context.getRequestId());
            response.setHeader(RequestHeaderConstants.TRACE_ID, context.getTraceId());
            filterChain.doFilter(request, response);
        } finally {
            clearMdc();
            GlobalRequestContextHolder.clear();
        }
    }

    private GlobalRequestContext buildContext(HttpServletRequest request) {
        String requestId = firstNonBlank(
                request.getHeader(RequestHeaderConstants.REQUEST_ID),
                UUID.randomUUID().toString()
        );
        String traceId = firstNonBlank(request.getHeader(RequestHeaderConstants.TRACE_ID), requestId);
        String timeZone = TimeZoneUtils.normalizeTimeZone(request.getHeader(RequestHeaderConstants.TIME_ZONE));
        String locale = LocaleUtils.normalizeAcceptLanguage(request.getHeader(RequestHeaderConstants.ACCEPT_LANGUAGE));

        return GlobalRequestContext.builder()
                .requestId(requestId)
                .traceId(traceId)
                .tenantId(blankToNull(request.getHeader(RequestHeaderConstants.TENANT_ID)))
                .userId(blankToNull(request.getHeader(RequestHeaderConstants.USER_ID)))
                .userName(blankToNull(request.getHeader(RequestHeaderConstants.USER_NAME)))
                .userRoles(blankToNull(request.getHeader(RequestHeaderConstants.USER_ROLES)))
                .callerId(blankToNull(request.getHeader(RequestHeaderConstants.CALLER_ID)))
                .locale(locale)
                .timeZone(timeZone)
                .ip(resolveClientIp(request))
                .url(request.getRequestURI())
                .methodType(request.getMethod())
                .build();
    }

    private void applyMdc(GlobalRequestContext context) {
        putMdc(MDC_REQUEST_ID, context.getRequestId());
        putMdc(MDC_TRACE_ID, context.getTraceId());
        putMdc(MDC_TENANT_ID, context.getTenantId());
        putMdc(MDC_USER_ID, context.getUserId());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void putMdc(String key, String value) {
        if (StringUtils.hasText(value)) {
            MDC.put(key, value);
        }
    }

    private void clearMdc() {
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_TRACE_ID);
        MDC.remove(MDC_TENANT_ID);
        MDC.remove(MDC_USER_ID);
    }

    private String firstNonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
