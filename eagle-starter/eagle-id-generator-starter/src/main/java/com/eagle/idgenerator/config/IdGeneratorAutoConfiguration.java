package com.eagle.idgenerator.config;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.idgenerator.generator.OrderNoGenerator;
import com.eagle.idgenerator.generator.SnowflakeIdGenerator;
import com.eagle.idgenerator.properties.IdGeneratorProperties;
import com.eagle.idgenerator.util.IdGeneratorFacade;
import com.eagle.idgenerator.util.IdGeneratorUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 分布式 ID 生成器自动配置。
 *
 * <p>默认使用雪花算法（{@link SnowflakeIdGenerator}）实现，可通过自定义 {@link IdGenerator} Bean 替换。
 * 可通过 {@code eagle.id-generator.enabled=false} 关闭。
 *
 * <p>注册以下 Bean：
 * <ul>
 *   <li>{@link IdGenerator} — 分布式 ID 生成器（雪花算法实现）</li>
 *   <li>{@link IdGeneratorUtil} — 静态工具类，持有生成器实例供非 Spring 场景使用</li>
 *   <li>{@link OrderNoGenerator} — 业务订单号生成器（含日期前缀，可通过 enableFacade 关闭）</li>
 *   <li>{@link IdGeneratorFacade} — 统一门面，聚合雪花 ID 和订单号生成（可通过 enableFacade 关闭）</li>
 * </ul>
 *
 * @author sunshixiong
 */
@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
@ConditionalOnProperty(name = "eagle.id-generator.enabled", havingValue = "true", matchIfMissing = true)
public class IdGeneratorAutoConfiguration {

    /**
     * 注册雪花算法 ID 生成器。
     *
     * <p>应用可自定义 {@link IdGenerator} Bean 以替换默认实现（如接入号段模式、UidGenerator 等）。
     *
     * @param properties ID 生成器配置属性
     * @return 雪花算法 ID 生成器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(IdGeneratorProperties properties) {
        return new SnowflakeIdGenerator(properties);
    }

    /**
     * 注册静态工具类 Bean，持有 {@link IdGenerator} 实例。
     *
     * @param idGenerator 分布式 ID 生成器
     * @return ID 生成器工具类实例
     */
    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorUtil idGeneratorUtil(IdGenerator idGenerator) {
        return new IdGeneratorUtil(idGenerator);
    }

    /**
     * 注册业务订单号生成器。
     *
     * <p>生成格式为 {@code {prefix}{yyyyMMdd}{9位序列}} 的可读业务单号，
     * 便于客服查询和对账。可通过 {@code eagle.id-generator.enable-facade=false} 关闭。
     *
     * @param idGenerator 雪花算法 ID 生成器（提供随机序列基础）
     * @return 订单号生成器实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            name = "eagle.id-generator.enable-facade",
            havingValue = "true",
            matchIfMissing = true)
    public OrderNoGenerator orderNoGenerator(IdGenerator idGenerator) {
        return new OrderNoGenerator(idGenerator);
    }

    /**
     * 注册 ID 生成器统一门面。
     *
     * <p>聚合雪花 ID 和订单号生成能力，提供 {@link IdGeneratorFacade#payNo()} /
     * {@link IdGeneratorFacade#refundNo()} 等语义化业务方法。
     * 可通过 {@code eagle.id-generator.enable-facade=false} 关闭。
     *
     * @param idGenerator      分布式 ID 生成器
     * @param orderNoGenerator 订单号生成器
     * @return ID 生成器门面实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            name = "eagle.id-generator.enable-facade",
            havingValue = "true",
            matchIfMissing = true)
    public IdGeneratorFacade idGeneratorFacade(
            IdGenerator idGenerator, OrderNoGenerator orderNoGenerator) {
        return new IdGeneratorFacade(idGenerator, orderNoGenerator);
    }
}
