package com.eagle.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 异步任务执行器配置（{@code eagle.async.*}）。
 *
 * <p>统一约束 {@code @Async} 方法所用 {@code taskExecutor} 的线程命名、并发上限与优雅停机等待时长。
 * 执行器选型由 Spring Boot 的 {@code spring.threads.virtual.enabled} 决定：
 * <ul>
 *   <li>{@code false}（默认）— 平台线程池（{@code ThreadPoolTaskExecutor}，有界队列 + CallerRunsPolicy 背压）</li>
 *   <li>{@code true}        — 虚拟线程执行器（{@code SimpleAsyncTaskExecutor} + 虚拟线程），并发上限由
 *       {@link #concurrencyLimit} 控制</li>
 * </ul>
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.async")
public class EagleAsyncProperties {

    /**
     * 异步线程名前缀（平台线程池与虚拟线程执行器共用）。
     */
    private String threadNamePrefix = "eagle-async-";

    /**
     * 虚拟线程执行器的并发上限（仅 {@code spring.threads.virtual.enabled=true} 时生效）。
     *
     * <p>虚拟线程本身极廉价、默认无界；当下游存在稀缺资源（如数据库连接池）需要背压时，
     * 设为正数即可：达到上限后，提交线程被节流阻塞，起到与平台线程池 {@code CallerRunsPolicy}
     * 相当的背压作用。{@code -1}（默认）表示不限并发，与 Spring Boot 虚拟线程 {@code @Async} 的默认行为一致。
     */
    private int concurrencyLimit = -1;

    /**
     * 优雅停机时等待进行中异步任务完成的最长时长（秒）。
     *
     * <p>平台线程池对应 {@code awaitTerminationSeconds}；虚拟线程执行器对应 {@code close()} 的
     * {@code taskTerminationTimeout}。避免应用关闭时丢失进行中的事件处理。
     */
    private int awaitTerminationSeconds = 30;
}
