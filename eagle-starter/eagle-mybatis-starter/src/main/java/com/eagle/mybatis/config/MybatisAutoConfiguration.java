package com.eagle.mybatis.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.eagle.mybatis.handler.EagleMetaObjectHandler;
import com.eagle.mybatis.interceptor.MybatisSlowSqlInterceptor;
import com.eagle.mybatis.properties.MybatisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 自动配置类。
 *
 * <p>当类路径存在 {@link MybatisPlusAutoConfiguration} 且配置属性
 * {@code eagle.mybatis.enabled} 为 {@code true}（默认启用）时，激活此配置。
 *
 * <p>提供以下开箱即用能力：
 * <ul>
 *   <li>分页插件（{@link PaginationInnerInterceptor}），可通过 {@code eagle.mybatis.pagination-enabled} 开关</li>
 *   <li>乐观锁插件（{@link OptimisticLockerInnerInterceptor}），可通过 {@code eagle.mybatis.optimistic-locker-enabled} 开关</li>
 *   <li>防全表更新删除插件（{@link BlockAttackInnerInterceptor}），默认关闭</li>
 *   <li>审计字段自动填充（{@link EagleMetaObjectHandler}）</li>
 *   <li>慢 SQL 拦截日志（{@link MybatisSlowSqlInterceptor}）</li>
 * </ul>
 *
 * @author eagle
 * @see MybatisProperties
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(MybatisPlusAutoConfiguration.class)
@EnableConfigurationProperties(MybatisProperties.class)
@ConditionalOnProperty(name = "eagle.mybatis.enabled", havingValue = "true", matchIfMissing = true)
public class MybatisAutoConfiguration {

    /**
     * 注册 MyBatis-Plus 插件拦截器。
     *
     * <p>根据 {@link MybatisProperties} 中的开关，按需添加以下插件：
     * <ul>
     *   <li>分页插件：自动识别数据库方言</li>
     *   <li>乐观锁插件：处理 {@code @Version} 字段</li>
     *   <li>防全表攻击插件：阻止无 WHERE 条件的 UPDATE/DELETE</li>
     * </ul>
     *
     * @param properties MyBatis-Plus 配置属性
     * @return 配置好的 MyBatis-Plus 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        if (properties.isPaginationEnabled()) {
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor(properties.getDbType()));
            log.debug("[Eagle MyBatis] Pagination plugin enabled, db type: {}", properties.getDbType());
        }

        if (properties.isOptimisticLockerEnabled()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            log.debug("[Eagle MyBatis] Optimistic locker plugin enabled.");
        }

        if (properties.isBlockAttackEnabled()) {
            interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
            log.debug("[Eagle MyBatis] Block attack plugin enabled.");
        }

        return interceptor;
    }

    /**
     * 注册审计字段自动填充处理器。
     *
     * <p>在 INSERT/UPDATE 时自动填充 {@code createTime}、{@code updateTime}、
     * {@code createBy}、{@code updateBy} 字段。
     *
     * @return 审计字段填充处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public EagleMetaObjectHandler eagleMetaObjectHandler() {
        return new EagleMetaObjectHandler();
    }

    /**
     * 注册慢 SQL 拦截器。
     *
     * <p>执行时间超过 {@link MybatisProperties#getSlowSqlMillis()} 阈值的 SQL
     * 将以 WARN 级别记录日志。
     *
     * @param properties MyBatis-Plus 配置属性
     * @return 慢 SQL 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisSlowSqlInterceptor mybatisSlowSqlInterceptor(MybatisProperties properties) {
        return new MybatisSlowSqlInterceptor(properties.getSlowSqlMillis());
    }
}
