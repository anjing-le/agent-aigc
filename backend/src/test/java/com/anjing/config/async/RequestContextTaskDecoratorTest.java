package com.anjing.config.async;

import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestContextTaskDecoratorTest {

    private final RequestContextTaskDecorator taskDecorator = new RequestContextTaskDecorator();

    @AfterEach
    void tearDown() {
        GlobalRequestContextHolder.clear();
        MDC.clear();
    }

    @Test
    void decorateShouldPropagateRequestContextAndMdcThenRestorePreviousValues() {
        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-parent")
                .traceId("tid-parent")
                .tenantId("tenant-a")
                .userId("user-a")
                .locale("zh-CN")
                .timeZone("Asia/Shanghai")
                .build());
        MDC.put("requestId", "rid-parent");
        MDC.put("traceId", "tid-parent");

        AtomicReference<String> propagatedRequestId = new AtomicReference<>();
        AtomicReference<String> propagatedTraceId = new AtomicReference<>();
        AtomicReference<String> propagatedMdcRequestId = new AtomicReference<>();
        AtomicReference<String> propagatedMdcTraceId = new AtomicReference<>();

        Runnable decorated = taskDecorator.decorate(() -> {
            propagatedRequestId.set(GlobalRequestContextHolder.requestIdOrEmpty());
            propagatedTraceId.set(GlobalRequestContextHolder.traceIdOrEmpty());
            propagatedMdcRequestId.set(MDC.get("requestId"));
            propagatedMdcTraceId.set(MDC.get("traceId"));

            GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                    .requestId("rid-inner")
                    .traceId("tid-inner")
                    .build());
            MDC.put("requestId", "rid-inner");
            MDC.put("traceId", "tid-inner");
        });

        GlobalRequestContextHolder.set(GlobalRequestContext.builder()
                .requestId("rid-worker")
                .traceId("tid-worker")
                .build());
        MDC.put("requestId", "rid-worker");
        MDC.put("traceId", "tid-worker");

        decorated.run();

        assertEquals("rid-parent", propagatedRequestId.get());
        assertEquals("tid-parent", propagatedTraceId.get());
        assertEquals("rid-parent", propagatedMdcRequestId.get());
        assertEquals("tid-parent", propagatedMdcTraceId.get());
        assertEquals("rid-worker", GlobalRequestContextHolder.requestIdOrEmpty());
        assertEquals("tid-worker", GlobalRequestContextHolder.traceIdOrEmpty());
        assertEquals("rid-worker", MDC.get("requestId"));
        assertEquals("tid-worker", MDC.get("traceId"));
    }
}
