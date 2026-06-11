package com.anjing.config.async;

import com.anjing.config.properties.AsyncExecutorProperties;
import com.anjing.context.GlobalRequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Central async executor for @Async, background tasks, and future adapters.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncExecutorProperties properties;
    private final RequestContextTaskDecorator requestContextTaskDecorator;

    @Bean(name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(properties.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        executor.setTaskDecorator(requestContextTaskDecorator);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (exception, method, params) -> log.error(
                "ASYNC_TASK_ERROR | requestId={} | traceId={} | method={} | error={}",
                GlobalRequestContextHolder.requestIdOrEmpty(),
                GlobalRequestContextHolder.traceIdOrEmpty(),
                method.getName(),
                exception.getMessage(),
                exception
        );
    }
}
