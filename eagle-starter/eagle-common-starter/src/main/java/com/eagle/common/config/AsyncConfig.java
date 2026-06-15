package com.eagle.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
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
 * 执行器选型由 Spring Boot 的 {@code spring.threads.virtual.enabled} 决定：
 * <ul>
 *   <li>{@code false}（默认）— 平台线程池（{@code ThreadPoolTaskExecutor}：有界队列 + CallerRunsPolicy 背压），
 *       行为与历史版本完全一致</li>
 *   <li>{@code true}        — 虚拟线程执行器（{@code SimpleAsyncTaskExecutor} + 虚拟线程），并发上限由
 *       {@code eagle.async.concurrency-limit} 控制（默认无界，与 Spring Boot 虚拟线程 {@code @Async} 默认行为一致）</li>
 * </ul>
 * 由此让 {@code @Async} 与 Tomcat 请求处理、{@code @Scheduled} 调度一起跑在虚拟线程上。
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
@RequiredArgsConstructor
@AutoConfiguration(before = TaskExecutionAutoConfiguration.class)
@EnableConfigurationProperties(EagleAsyncProperties.class)
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 开启虚拟线程的 Spring Boot 标准开关。
     */
    private static final String VIRTUAL_THREADS_PROPERTY = "spring.threads.virtual.enabled";

    /**
     * 核心线程数：CPU 核心数（仅平台线程池模式生效）
     */
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程数：核心线程数 × 2（仅平台线程池模式生效）
     */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    /**
     * 队列容量：超过此值触发扩容到 MAX_POOL_SIZE，再满则执行拒绝策略（仅平台线程池模式生效）
     */
    private static final int QUEUE_CAPACITY = 200;

    /**
     * 空闲线程存活时间（秒）（仅平台线程池模式生效）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    private final Environment environment;

    private final EagleAsyncProperties properties;

    /**
     * 缓存唯一的执行器实例。
     * <p>
     * {@code @AutoConfiguration} 默认 {@code proxyBeanMethods = false}，
     * 在 {@link #getAsyncExecutor()} 既作为 {@code @Bean} 工厂方法、又作为
     * {@code AsyncConfigurer} 接口实现被调用的场景下，会创建两份执行器实例。
     * 用 {@code volatile + 双检锁} 保证整个 ApplicationContext 内只有一个实例。
     */
    private volatile Executor cachedExecutor;

    /**
     * 默认异步任务执行器，Bean 名称为 "taskExecutor"
     * <p>
     * 平台线程池模式拒绝策略使用 CallerRunsPolicy：提交线程自己执行任务，
     * 起到背压作用，防止任务无限积压丢失。虚拟线程模式则通过并发上限节流实现背压。
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        Executor executor = cachedExecutor;
        if (executor == null) {
            synchronized (this) {
                executor = cachedExecutor;
                if (executor == null) {
                    executor = virtualThreadsEnabled()
                            ? buildVirtualThreadExecutor()
                            : buildPlatformThreadPoolExecutor();
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

    private boolean virtualThreadsEnabled() {
        return environment.getProperty(VIRTUAL_THREADS_PROPERTY, Boolean.class, Boolean.FALSE);
    }

    /**
     * 虚拟线程执行器：每个任务一根虚拟线程，可选并发上限做背压。
     */
    private SimpleAsyncTaskExecutor buildVirtualThreadExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(properties.getThreadNamePrefix());
        executor.setVirtualThreads(true);
        int concurrencyLimit = properties.getConcurrencyLimit();
        if (concurrencyLimit > 0) {
            // 达到上限后提交线程被节流阻塞，等价于平台线程池的 CallerRunsPolicy 背压
            executor.setConcurrencyLimit(concurrencyLimit);
        }
        // 优雅关闭：close() 最多等待进行中的虚拟线程任务完成，避免应用关闭时丢失事件
        executor.setTaskTerminationTimeout(properties.getAwaitTerminationSeconds() * 1000L);
        log.info("异步执行器初始化完成(虚拟线程): namePrefix={}, concurrencyLimit={}",
                properties.getThreadNamePrefix(),
                concurrencyLimit > 0 ? concurrencyLimit : "unbounded");
        return executor;
    }

    /**
     * 平台线程池执行器：有界队列 + CallerRunsPolicy 背压（历史默认行为）。
     */
    private ThreadPoolTaskExecutor buildPlatformThreadPoolExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix(properties.getThreadNamePrefix());
        // 拒绝策略：调用者线程执行，起背压作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务完成再关闭，避免应用关闭时丢失事件
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        executor.initialize();
        log.info("异步线程池初始化完成(平台线程): core={}, max={}, queue={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        return executor;
    }
}
