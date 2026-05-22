package com.eagle.seata.config;

import com.eagle.seata.properties.SeataProperties;
import com.eagle.seata.tcc.TccIdempotencyHelper;
import com.eagle.seata.transaction.GlobalTransactionTemplate;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Eagle Seata 分布式事务自动配置。
 *
 * <p>在类路径存在 {@code org.apache.seata.core.context.RootContext} 时自动激活。
 * 本配置类主要作为入口点，负责：
 * <ul>
 *   <li>启用 {@link SeataProperties} 属性绑定</li>
 *   <li>在应用启动时打印 Seata 已启用日志</li>
 *   <li>注册 {@link TccIdempotencyHelper} — TCC Confirm/Cancel 幂等辅助工具</li>
 *   <li>注册 {@link GlobalTransactionTemplate} — 编程式全局事务模板</li>
 * </ul>
 *
 * <p>实际 Seata 核心组件（{@code GlobalTransactionScanner} 等）由
 * {@code seata-spring-boot-starter} 的自动配置负责注册，本模块不重复注册。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.seata.core.context.RootContext")
@EnableConfigurationProperties(SeataProperties.class)
public class SeataAutoConfiguration {

    /**
     * Seata 配置属性，用于启动日志输出。
     */
    private final SeataProperties seataProperties;

    /**
     * 打印 Seata 启动配置信息，便于排查配置问题。
     */
    @PostConstruct
    public void init() {
        log.info("[Eagle Seata] Seata distributed transaction enabled. "
                        + "applicationId={}, txServiceGroup={}",
                seataProperties.getApplicationId(),
                seataProperties.getTxServiceGroup());
    }

    /**
     * TCC 阶段幂等辅助工具。
     *
     * <p>提供 {@code isConfirmed / isCancelled / markConfirmed / markCancelled} 方法，
     * 解决 Seata 在失败重试时 Confirm/Cancel 被多次调用的幂等问题。
     * 默认实现基于内存 {@code ConcurrentHashMap}，生产环境可声明自定义 Bean 替换为
     * Redis 或数据库持久化实现。
     *
     * @return TccIdempotencyHelper 实例
     */
    @Bean
    @ConditionalOnMissingBean(TccIdempotencyHelper.class)
    public TccIdempotencyHelper tccIdempotencyHelper() {
        log.info("[Eagle Seata] TccIdempotencyHelper registered (in-memory implementation)");
        return new TccIdempotencyHelper();
    }

    /**
     * 编程式 Seata 全局事务模板。
     *
     * <p>提供 {@code execute(txName, callback)} API，适用于无法使用
     * {@code @GlobalTransactional} 注解的场景（框架集成、循环事务、测试等）。
     * 可通过声明自定义 {@link GlobalTransactionTemplate} Bean 覆盖默认实现。
     *
     * @return GlobalTransactionTemplate 实例
     */
    @Bean
    @ConditionalOnMissingBean(GlobalTransactionTemplate.class)
    public GlobalTransactionTemplate globalTransactionTemplate() {
        log.info("[Eagle Seata] GlobalTransactionTemplate registered");
        return new GlobalTransactionTemplate();
    }
}
