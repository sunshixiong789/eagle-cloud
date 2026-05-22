package com.eagle.datapermission.config;

import com.eagle.datapermission.aspect.DataPermissionAspect;
import com.eagle.datapermission.properties.DataPermissionProperties;
import com.eagle.datapermission.provider.DataPermissionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据权限自动配置。
 *
 * <p>生效条件：容器中存在 {@link DataPermissionProvider} Bean（需业务方实现并注册）。
 *
 * <p>若需替换默认切面，声明自定义 {@link DataPermissionAspect} Bean 即可覆盖。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DataPermissionProperties.class)
public class DataPermissionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataPermissionProvider.class)
    public DataPermissionAspect dataPermissionAspect(DataPermissionProvider provider,
                                                     DataPermissionProperties properties) {
        log.info("Data permission aspect initialized, defaultDeptField: {}, defaultUserField: {}",
                properties.getDefaultDeptField(), properties.getDefaultUserField());
        return new DataPermissionAspect(provider, properties);
    }
}