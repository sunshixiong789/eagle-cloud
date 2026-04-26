package com.eagle.datapermission.config;

import com.eagle.datapermission.aspect.DataPermissionAspect;
import com.eagle.datapermission.properties.DataPermissionProperties;
import com.eagle.datapermission.provider.DataPermissionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据权限自动配置。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DataPermissionProperties.class)
@ConditionalOnProperty(name = "eagle.data-permission.enabled", havingValue = "true", matchIfMissing = true)
public class DataPermissionAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataPermissionProvider.class)
    public DataPermissionAspect dataPermissionAspect(DataPermissionProvider provider) {
        log.info("Data permission aspect initialized");
        return new DataPermissionAspect(provider);
    }
}
