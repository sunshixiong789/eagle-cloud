package com.eagle.xxljob.config;

import com.eagle.xxljob.properties.XxlJobProperties;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * XXL-Job 执行器自动配置。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnProperty(prefix = "eagle.xxl-job", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobAutoConfiguration {

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor(XxlJobProperties properties) {
        log.info("XXL-Job executor starting, appName: {}, adminAddresses: {}",
                properties.getAppName(), properties.getAdminAddresses());

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(properties.getAdminAddresses());
        executor.setAccessToken(properties.getAccessToken());
        executor.setAppname(properties.getAppName());
        executor.setIp(properties.getIp());
        executor.setPort(properties.getPort());
        executor.setLogPath(properties.getLogPath());
        executor.setLogRetentionDays(properties.getLogRetentionDays());

        return executor;
    }
}
