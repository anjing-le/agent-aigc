package com.anjing.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Async executor defaults for @Async and future background adapters.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.async")
public class AsyncExecutorProperties {

    /**
     * Core worker count.
     */
    private int corePoolSize = 4;

    /**
     * Maximum worker count.
     */
    private int maxPoolSize = 16;

    /**
     * Queue capacity before growing workers.
     */
    private int queueCapacity = 200;

    /**
     * Idle worker keep-alive time.
     */
    private int keepAliveSeconds = 60;

    /**
     * Thread name prefix for logs and diagnostics.
     */
    private String threadNamePrefix = "anjing-async-";

    /**
     * Whether shutdown should wait for queued tasks.
     */
    private boolean waitForTasksToCompleteOnShutdown = true;

    /**
     * Graceful shutdown wait time.
     */
    private int awaitTerminationSeconds = 30;
}
