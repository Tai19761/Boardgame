package com.project.boardgame.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pool riêng cho tác vụ AI (CPU-bound), định cỡ theo số nhân CPU:
 * {@code Math.max(2, availableProcessors() - 1)} — chừa lại 1 nhân cho luồng web/WebSocket.
 */
@Configuration
public class AiWorkerConfig {

    static int calculatePoolSize(int availableProcessors) {
        return Math.max(2, availableProcessors - 1);
    }

    @Bean(name = "aiExecutorService", destroyMethod = "shutdown")
    public ExecutorService aiExecutorService() {
        int poolSize = calculatePoolSize(Runtime.getRuntime().availableProcessors());
        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "ai-worker-" + counter.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };
        return Executors.newFixedThreadPool(poolSize, threadFactory);
    }
}
