package com.eagle.payment.config;

import com.eagle.payment.controller.PaymentNotifyController;
import com.eagle.payment.gateway.PaymentGateway;
import com.eagle.payment.gateway.alipay.AlipayPaymentGateway;
import com.eagle.payment.gateway.wechat.WechatPaymentGateway;
import com.eagle.payment.properties.PaymentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付统一自动配置。
 *
 * <p>支付宝和微信支付均以可选依赖接入，只有对应 SDK 在类路径且配置了必要参数时才会激活：
 * <ul>
 *   <li>支付宝：{@code eagle.payment.alipay.app-id} 存在且 Alipay SDK 在类路径</li>
 *   <li>微信支付：{@code eagle.payment.wechat.mch-id} 存在且 WeChatPay SDK 在类路径</li>
 * </ul>
 *
 * <p>如需替换默认实现，在应用上下文中注册同类型 Bean 即可，
 * {@code @ConditionalOnMissingBean} 保证不会产生冲突。
 *
 * @author eagle
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentAutoConfiguration {

    /**
     * 支付宝配置内部类。
     *
     * <p>需要 {@code com.alipay.api.AlipayClient} 在类路径（即引入 alipay-sdk-java 依赖），
     * 且配置了 {@code eagle.payment.alipay.app-id} 时才激活。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.alipay.api.AlipayClient")
    @ConditionalOnProperty(prefix = "eagle.payment.alipay", name = "app-id")
    static class AlipayConfiguration {

        /**
         * 注册支付宝支付网关 Bean。
         *
         * @param properties 支付配置
         * @return 支付宝支付网关
         */
        @Bean
        @ConditionalOnMissingBean(name = "alipayPaymentGateway")
        AlipayPaymentGateway alipayPaymentGateway(PaymentProperties properties) {
            log.info("AlipayPaymentGateway enabled, appId: {}", properties.getAlipay().getAppId());
            return new AlipayPaymentGateway(properties.getAlipay());
        }
    }

    /**
     * 微信支付配置内部类。
     *
     * <p>需要 {@code com.wechat.pay.java.core.RSAAutoCertificateConfig} 在类路径
     * （即引入 wechatpay-java 依赖），且配置了 {@code eagle.payment.wechat.mch-id} 时才激活。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.wechat.pay.java.core.RSAAutoCertificateConfig")
    @ConditionalOnProperty(prefix = "eagle.payment.wechat", name = "mch-id")
    static class WechatPayConfiguration {

        /**
         * 注册微信支付网关 Bean。
         *
         * @param properties 支付配置
         * @return 微信支付网关
         */
        @Bean
        @ConditionalOnMissingBean(name = "wechatPaymentGateway")
        WechatPaymentGateway wechatPaymentGateway(PaymentProperties properties) {
            log.info("WechatPaymentGateway enabled, mchId: {}", properties.getWechat().getMchId());
            return new WechatPaymentGateway(properties.getWechat());
        }
    }

    /**
     * 注册支付回调接收控制器。
     *
     * <p>提供 {@code POST /payment/alipay/notify} 和 {@code POST /payment/wechat/notify}
     * 两个端点，验签后发布 {@link com.eagle.payment.event.PaymentNotifyEvent}。
     * 仅在 Servlet 环境下激活。
     *
     * @param alipayGatewayProvider 支付宝网关 Provider（可选）
     * @param wechatGatewayProvider 微信支付网关 Provider（可选）
     * @param eventPublisher        Spring 事件发布器
     * @return 支付回调控制器
     */
    @Bean
    @ConditionalOnMissingBean(PaymentNotifyController.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public PaymentNotifyController paymentNotifyController(
            @Qualifier("alipayPaymentGateway") ObjectProvider<PaymentGateway> alipayGatewayProvider,
            @Qualifier("wechatPaymentGateway") ObjectProvider<PaymentGateway> wechatGatewayProvider,
            ApplicationEventPublisher eventPublisher) {
        return new PaymentNotifyController(alipayGatewayProvider, wechatGatewayProvider, eventPublisher);
    }
}
