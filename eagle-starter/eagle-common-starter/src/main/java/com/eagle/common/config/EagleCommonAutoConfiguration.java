package com.eagle.common.config;

import com.eagle.common.handler.GlobalExceptionHandler;
import com.eagle.common.i18n.MessageSourceUtil;
import com.eagle.common.metrics.BusinessMetrics;
import com.eagle.common.pressuretest.PressureTestFilter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * Eagle 通用基础设施自动配置。
 *
 * <p>注册 common-starter 中需要自动装配的 Bean，
 * 使得消费方无需 {@code @ComponentScan("com.eagle.common")} 即可使用。
 *
 * <p>实现 {@link InitializingBean} 以在容器就绪后初始化 {@link MessageSourceUtil}，
 * 使其在无法注入的静态场景（枚举、工具类）中也能访问 i18n 消息。
 *
 * @author sunshixiong
 */
@AutoConfiguration
@RequiredArgsConstructor
public class EagleCommonAutoConfiguration implements InitializingBean {

    private final MessageSource messageSource;

    @Override
    public void afterPropertiesSet() {
        MessageSourceUtil.init(messageSource);
    }

    /**
     * Servlet（WebMVC）环境专用配置。
     *
     * <p>{@link GlobalExceptionHandler} 依赖 {@code HttpServletRequest}，
     * {@link I18nConfig} 使用 {@code LocaleResolver}，
     * 两者在 WebFlux（Gateway）环境中不可用，必须限定 Servlet 类型。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Import({I18nConfig.class, GlobalExceptionHandler.class})
    static class WebMvcConfiguration {

        /**
         * 全链路压测流量识别过滤器。
         * 优先级仅次于租户过滤器，确保压测标记在所有业务过滤器前就绪。
         */
        @Bean
        @ConditionalOnMissingBean
        public FilterRegistrationBean<PressureTestFilter> pressureTestFilter() {
            FilterRegistrationBean<PressureTestFilter> bean = new FilterRegistrationBean<>();
            bean.setFilter(new PressureTestFilter());
            bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
            bean.addUrlPatterns("/*");
            return bean;
        }
    }

    /**
     * 业务指标监控（依赖 Micrometer MeterRegistry，仅在 Actuator 存在时生效）。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry.class)
    static class MetricsConfiguration {

        /**
         * 业务指标工具 Bean，供各业务模块注入使用。
         */
        @Bean
        @ConditionalOnMissingBean
        public BusinessMetrics businessMetrics(MeterRegistry registry) {
            return new BusinessMetrics(registry);
        }
    }
}