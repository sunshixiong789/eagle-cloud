package com.eagle.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
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
 * <p>
 * 必须使用 {@code @AutoConfiguration(before = TaskExecutionAutoConfiguration.class)}：
 * Spring Boot 4 的 {@code TaskExecutorConfigurations$AsyncConfigurerConfiguration} 通过
 * {@code @ConditionalOnMissingBean(AsyncConfigurer.class)} 在缺省时注册
 * {@code applicationTaskExecutorAsyncConfigurer}。如果本类用 {@code @Configuration}
 * 加载，时机晚于 auto-config，导致默认 configurer 先注册、本类再注册 → 出现两个
 * {@code AsyncConfigurer} bean，触发 "Only one AsyncConfigurer may exist"。
 *
 * @author sunshixiong
 */
@Slf4j
@EnableAsync
@EnableScheduling
@AutoConfiguration(before = TaskExecutionAutoConfiguration.class)
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 核心线程数：CPU 核心数
     */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程数：核心线程数 × 2
     */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    /**
     * 队列容量：超过此值触发扩容到 MAX_POOL_SIZE，再满则执行拒绝策略
     */
    private static final int QUEUE_CAPACITY = 200;

    /**
     * 空闲线程存活时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 缓存唯一的执行器实例。
     * <p>
     * {@code @AutoConfiguration} 默认 {@code proxyBeanMethods = false}，
     * 在 {@link #getAsyncExecutor()} 既作为 {@code @Bean} 工厂方法、又作为
     * {@code AsyncConfigurer} 接口实现被调用的场景下，会创建两份线程池实例。
     * 用 {@code volatile + 双检锁} 保证整个 ApplicationContext 内只有一个实例。
     */
    private volatile ThreadPoolTaskExecutor cachedExecutor;

    /**
     * 默认异步任务执行器，Bean 名称为 "taskExecutor"
     * <p>
     * 拒绝策略使用 CallerRunsPolicy：提交线程自己执行任务，
     * 起到背压作用，防止任务无限积压丢失。
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = cachedExecutor;
        if (executor == null) {
            synchronized (this) {
                executor = cachedExecutor;
                if (executor == null) {
                    executor = new ThreadPoolTaskExecutor();
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
                    cachedExecutor = executor;
                }
            }
        }
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
