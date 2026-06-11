package com.anjing.config.async;

import com.anjing.context.GlobalRequestContextHolder;
import com.anjing.model.request.GlobalRequestContext;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Propagates request context and MDC into async executor threads.
 */
@Component
public class RequestContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        GlobalRequestContext contextSnapshot = GlobalRequestContextHolder.capture().orElse(null);
        Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();

        return () -> {
            GlobalRequestContext previousContext = GlobalRequestContextHolder.capture().orElse(null);
            Map<String, String> previousMdc = MDC.getCopyOfContextMap();

            try {
                GlobalRequestContextHolder.setOrClear(contextSnapshot);
                applyMdc(mdcSnapshot);
                runnable.run();
            } finally {
                GlobalRequestContextHolder.setOrClear(previousContext);
                applyMdc(previousMdc);
            }
        };
    }

    private void applyMdc(Map<String, String> contextMap) {
        MDC.clear();
        if (contextMap != null && !contextMap.isEmpty()) {
            MDC.setContextMap(contextMap);
        }
    }
}
