package com.eagle.common.config;

import com.eagle.common.handler.GlobalExceptionHandler;
import com.eagle.common.i18n.MessageSourceUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Eagle 通用基础设施自动配置
 * <p>
 * 注册 common-starter 中需要自动装配的 Bean，
 * 使得消费方无需 {@code @ComponentScan("com.eagle.common")} 即可使用。
 *
 * @author sunshixiong
 */
@AutoConfiguration
@Import(MessageSourceUtil.class)
public class EagleCommonAutoConfiguration {

    /**
     * Servlet（WebMVC）环境专用配置
     * <p>
     * {@link GlobalExceptionHandler} 依赖 {@code HttpServletRequest}，
     * {@link I18nConfig} 实现 {@code WebMvcConfigurer}，
     * 两者在 WebFlux（Gateway）环境中不可用，必须限定 Servlet 类型。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @Import({I18nConfig.class, GlobalExceptionHandler.class})
    static class WebMvcConfiguration {
    }
}
