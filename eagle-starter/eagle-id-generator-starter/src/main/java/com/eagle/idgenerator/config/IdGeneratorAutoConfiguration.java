package com.eagle.idgenerator.config;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.idgenerator.generator.NanoIdGenerator;
import com.eagle.idgenerator.generator.OrderNoGenerator;
import com.eagle.idgenerator.generator.SnowflakeIdGenerator;
import com.eagle.idgenerator.generator.TsidIdGenerator;
import com.eagle.idgenerator.generator.UuidIdGenerator;
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
 * <p>注册以下 Bean：
 * <ul>
 *   <li>{@link SnowflakeIdGenerator} — 雪花算法（Hutool）</li>
 *   <li>{@link UuidIdGenerator} — UUID v7（uuid-creator）</li>
 *   <li>{@link TsidIdGenerator} — TSID（tsid-creator）</li>
 *   <li>{@link NanoIdGenerator} — NanoId 短字符串（Hutool）</li>
 *   <li>{@link IdGenerator} — 默认 long ID 生成器，由 {@code eagle.id-generator.type} 决定</li>
 *   <li>{@link IdGeneratorUtil} — 静态工具类</li>
 *   <li>{@link OrderNoGenerator} / {@link IdGeneratorFacade} — 业务门面（可关闭）</li>
 * </ul>
 *
 * <p>可通过 {@code eagle.id-generator.enabled=false} 整体关闭，
 * 或通过 {@code eagle.id-generator.type=snowflake|uuid|tsid} 切换默认实现。
 *
 * @author sunshixiong
 */
@AutoConfiguration
@EnableConfigurationProperties(IdGeneratorProperties.class)
@ConditionalOnProperty(name = "eagle.id-generator.enabled", havingValue = "true", matchIfMissing = true)
public class IdGeneratorAutoConfiguration {

    /**
     * 雪花算法生成器（始终注册，可被业务直接注入）
     */
    @Bean
    @ConditionalOnMissingBean
    public SnowflakeIdGenerator snowflakeIdGenerator(IdGeneratorProperties properties) {
        return new SnowflakeIdGenerator(properties);
    }

    /**
     * UUID v7 生成器（始终注册，可被业务直接注入）
     */
    @Bean
    @ConditionalOnMissingBean
    public UuidIdGenerator uuidIdGenerator() {
        return new UuidIdGenerator();
    }

    /**
     * TSID 生成器（始终注册，可被业务直接注入）
     */
    @Bean
    @ConditionalOnMissingBean
    public TsidIdGenerator tsidIdGenerator(IdGeneratorProperties properties) {
        IdGeneratorProperties.Tsid tsid = properties.getTsid();
        return new TsidIdGenerator(tsid.getNodeId(), tsid.getNodeBits());
    }

    /**
     * NanoId 生成器（始终注册，可被业务直接注入）
     */
    @Bean
    @ConditionalOnMissingBean
    public NanoIdGenerator nanoIdGenerator(IdGeneratorProperties properties) {
        return new NanoIdGenerator(properties.getNanoId().getDefaultSize());
    }

    /**
     * 默认 {@link IdGenerator} Bean，按 {@code eagle.id-generator.type} 选择实现。
     *
     * <p>业务通过注入 {@code IdGenerator} 接口获取（推荐），如需明确实现则注入具体类型 Bean。
     */
    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(
            IdGeneratorProperties properties,
            SnowflakeIdGenerator snowflakeIdGenerator,
            UuidIdGenerator uuidIdGenerator,
            TsidIdGenerator tsidIdGenerator) {
        return switch (properties.getType()) {
            case UUID -> uuidIdGenerator;
            case TSID -> tsidIdGenerator;
            case SNOWFLAKE -> snowflakeIdGenerator;
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorUtil idGeneratorUtil(
            IdGenerator idGenerator,
            UuidIdGenerator uuidIdGenerator,
            TsidIdGenerator tsidIdGenerator,
            NanoIdGenerator nanoIdGenerator) {
        return new IdGeneratorUtil(idGenerator, uuidIdGenerator, tsidIdGenerator, nanoIdGenerator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            name = "eagle.id-generator.enable-facade",
            havingValue = "true",
            matchIfMissing = true)
    public OrderNoGenerator orderNoGenerator(IdGenerator idGenerator) {
        return new OrderNoGenerator(idGenerator);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            name = "eagle.id-generator.enable-facade",
            havingValue = "true",
            matchIfMissing = true)
    public IdGeneratorFacade idGeneratorFacade(
            IdGenerator idGenerator,
            UuidIdGenerator uuidIdGenerator,
            TsidIdGenerator tsidIdGenerator,
            NanoIdGenerator nanoIdGenerator,
            OrderNoGenerator orderNoGenerator) {
        return new IdGeneratorFacade(
                idGenerator, uuidIdGenerator, tsidIdGenerator, nanoIdGenerator, orderNoGenerator);
    }
}
