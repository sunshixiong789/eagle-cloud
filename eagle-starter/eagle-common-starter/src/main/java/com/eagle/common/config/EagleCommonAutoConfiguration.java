package com.eagle.common.config;

import com.eagle.common.handler.GlobalExceptionHandler;
import com.eagle.common.handler.ReactiveGlobalExceptionHandler;
import com.eagle.common.i18n.MessageSourceUtil;
import com.eagle.common.metrics.BusinessMetrics;
import com.eagle.common.observability.ContextPropagationConfig;
import com.eagle.common.observability.RequestIdMdcFilter;
import com.eagle.common.observability.RequestIdWebFilter;
import com.eagle.common.pressuretest.PressureTestFilter;
import com.eagle.common.pressuretest.ReactivePressureTestWebFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.WebFilter;

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

        /**
         * Request ID 解析 + MDC 注入过滤器。
         *
         * <p>order = HIGHEST_PRECEDENCE：必须最先执行,确保后续所有过滤器、业务代码、
         * 异常处理（{@code ErrorResult.of} 内部读 MDC）都能拿到 requestId。
         */
        @Bean
        @ConditionalOnMissingBean
        public FilterRegistrationBean<RequestIdMdcFilter> requestIdMdcFilter() {
            FilterRegistrationBean<RequestIdMdcFilter> bean = new FilterRegistrationBean<>();
            bean.setFilter(new RequestIdMdcFilter());
            bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
            bean.addUrlPatterns("/*");
            return bean;
        }
    }

    /**
     * Reactive（WebFlux）环境专用配置。
     *
     * <p>注册响应式 RequestId / 压测 WebFilter、统一异常处理器，以及 Reactor Context 自动传播。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(DispatcherHandler.class)
    @Import(ContextPropagationConfig.class)
    static class WebFluxConfiguration {

        /**
         * Request ID 解析 + 响应头回写过滤器。
         */
        @Bean
        @ConditionalOnMissingBean(RequestIdWebFilter.class)
        public WebFilter requestIdWebFilter() {
            return new RequestIdWebFilter();
        }

        /**
         * 全链路压测流量识别过滤器（WebFlux）。
         * 与 servlet 端 {@link PressureTestFilter} 行为对齐，保证压测标记跨 WebFlux 节点不断。
         */
        @Bean
        @ConditionalOnMissingBean(ReactivePressureTestWebFilter.class)
        public WebFilter reactivePressureTestWebFilter() {
            return new ReactivePressureTestWebFilter();
        }

        /**
         * WebFlux JSON 异常处理器。
         */
        @Bean
        @ConditionalOnMissingBean(ReactiveGlobalExceptionHandler.class)
        public ReactiveGlobalExceptionHandler reactiveGlobalExceptionHandler(
                ObjectMapper objectMapper,
                MessageSource messageSource) {
            return new ReactiveGlobalExceptionHandler(objectMapper, messageSource);
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
