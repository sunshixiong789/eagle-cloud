package com.eagle.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * <p>
 * 统一管理应用内所有异步任务的线程池参数，避免使用默认的无界线程池。
 * 领域事件处理（{@code @Async} + {@code @TransactionalEventListener}）
 * 及其他 {@code @Async} 方法均使用此线程池。
 *
 * @author sunshixiong
 */
@Slf4j
@EnableAsync
@EnableScheduling
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /** 核心线程数：CPU 核心数 */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /** 最大线程数：核心线程数 × 2 */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    /** 队列容量：超过此值触发扩容到 MAX_POOL_SIZE，再满则执行拒绝策略 */
    private static final int QUEUE_CAPACITY = 200;

    /** 空闲线程存活时间（秒） */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 默认异步任务执行器，Bean 名称为 "taskExecutor"
     * <p>
     * 拒绝策略使用 CallerRunsPolicy：提交线程自己执行任务，
     * 起到背压作用，防止任务无限积压丢失。
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("eagle-async-");
        // 拒绝策略：调用者线程执行，起背压作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成再关闭，避免应用关闭时丢失事件
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("异步线程池初始化完成: core={}, max={}, queue={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        return executor;
    }

    /**
     * 异步任务未捕获异常处理器
     * <p>
     * 记录异步方法中未被捕获的异常，防止异常被静默吞掉。
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
