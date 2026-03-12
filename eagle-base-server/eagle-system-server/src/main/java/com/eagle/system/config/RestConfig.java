package com.eagle.system.config;

import org.jspecify.annotations.NonNull;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

/**
 * spring  data rest配置
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/11/18-19:47
 */
//@Configuration
public class RestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, @NonNull CorsRegistry cors) {
        // 只暴露带 @RepositoryRestResource 的 Repository
        config.setRepositoryDetectionStrategy(RepositoryDetectionStrategy.RepositoryDetectionStrategies.ANNOTATED);

        // 全局禁用 DELETE（可选）
        config.getExposureConfiguration()
                .forDomainType(Object.class)
                .withItemExposure((metadata, httpMethods) ->
                        httpMethods.disable(HttpMethod.DELETE));
    }
}