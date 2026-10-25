package com.project.boardgame.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

class AiWorkerConfigTest {

    @Test
    @DisplayName("Công thức pool: Math.max(2, availableProcessors - 1)")
    void calculatePoolSize_FollowsFormula() {
        assertEquals(2, AiWorkerConfig.calculatePoolSize(1));
        assertEquals(2, AiWorkerConfig.calculatePoolSize(2));
        assertEquals(2, AiWorkerConfig.calculatePoolSize(3));
        assertEquals(7, AiWorkerConfig.calculatePoolSize(8));
        assertEquals(15, AiWorkerConfig.calculatePoolSize(16));
    }

    @Test
    @DisplayName("Bean ExecutorService là pool cố định đúng kích thước theo số CPU hiện tại")
    void aiExecutorService_HasExpectedPoolSize() {
        ExecutorService executor = new AiWorkerConfig().aiExecutorService();
        try {
            ThreadPoolExecutor pool = assertInstanceOf(ThreadPoolExecutor.class, executor);
            int expected = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
            assertEquals(expected, pool.getCorePoolSize());
            assertEquals(expected, pool.getMaximumPoolSize());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Bean khai báo destroyMethod = \"shutdown\" để giải phóng thread khi tắt ứng dụng")
    void aiExecutorService_DeclaresShutdownDestroyMethod() throws NoSuchMethodException {
        Method factoryMethod = AiWorkerConfig.class.getMethod("aiExecutorService");
        Bean bean = factoryMethod.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals("shutdown", bean.destroyMethod());
    }
}
