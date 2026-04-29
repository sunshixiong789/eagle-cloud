package com.eagle.payment.controller;

import com.eagle.payment.event.PaymentNotifyEvent;
import com.eagle.payment.gateway.PaymentGateway;
import com.eagle.payment.model.NotifyResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付异步回调接收控制器。
 *
 * <p>提供支付宝和微信支付的回调接收端点。回调验签成功后发布
 * {@link PaymentNotifyEvent}，由业务方监听处理，与支付基础设施解耦。
 *
 * <p>如需自定义回调处理逻辑，可注册同类型 Bean 覆盖此默认实现，
 * 自动配置通过 {@code @ConditionalOnMissingBean} 保证不冲突。
 *
 * @author eagle
 */
@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentNotifyController {

    private final ObjectProvider<PaymentGateway> alipayGatewayProvider;
    private final ObjectProvider<PaymentGateway> wechatGatewayProvider;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 构造支付回调控制器。
     *
     * <p>通过 {@link com.eagle.payment.config.PaymentAutoConfiguration} 工厂方法注入，
     * 由自动配置层传入带 {@code @Qualifier} 的 Provider，避免控制器层直接依赖 Bean 名称。
     *
     * @param alipayGatewayProvider  支付宝网关 Provider（可选，未配置时 getIfAvailable 返回 null）
     * @param wechatGatewayProvider  微信支付网关 Provider（可选，未配置时 getIfAvailable 返回 null）
     * @param eventPublisher         Spring 事件发布器
     */
    public PaymentNotifyController(
            ObjectProvider<PaymentGateway> alipayGatewayProvider,
            ObjectProvider<PaymentGateway> wechatGatewayProvider,
            ApplicationEventPublisher eventPublisher) {
        this.alipayGatewayProvider = alipayGatewayProvider;
        this.wechatGatewayProvider = wechatGatewayProvider;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 支付宝异步通知回调端点。
     *
     * <p>支付宝以 HTTP POST 表单形式推送通知，接收后完成验签并发布
     * {@link PaymentNotifyEvent}。验签失败返回 {@code "fail"}，
     * 成功返回 {@code "success"}（支付宝凭此判断是否重推）。
     *
     * @param request HTTP 请求
     * @return {@code "success"} 或 {@code "fail"}
     */
    @PostMapping("/alipay/notify")
    public ResponseEntity<String> alipayNotify(HttpServletRequest request) {
        PaymentGateway gateway = alipayGatewayProvider.getIfAvailable();
        if (gateway == null) {
            log.warn("AlipayPaymentGateway not configured, ignoring notify");
            return ResponseEntity.ok("fail");
        }

        Map<String, String> params = extractFormParams(request);
        log.debug("Received alipay notify, outTradeNo: {}", params.get("out_trade_no"));

        NotifyResult result = gateway.parseNotify(params, null);
        if (!result.isSuccess() && result.getOutTradeNo() == null) {
            // 验签失败
            log.warn("Alipay notify signature verification failed");
            return ResponseEntity.ok("fail");
        }

        eventPublisher.publishEvent(new PaymentNotifyEvent(this, result));
        log.info("Alipay notify published, outTradeNo: {}, success: {}",
                result.getOutTradeNo(), result.isSuccess());
        return ResponseEntity.ok("success");
    }

    /**
     * 微信支付异步通知回调端点。
     *
     * <p>微信以 HTTP POST JSON 形式推送通知，头部包含签名信息。
     * 验签成功后发布 {@link PaymentNotifyEvent}。
     * 返回 HTTP 200 表示已收到（微信凭 HTTP 状态码判断是否重推）。
     *
     * @param body    请求体 JSON
     * @param request HTTP 请求（含签名头部）
     * @return HTTP 200 空响应
     */
    @PostMapping("/wechat/notify")
    public ResponseEntity<Void> wechatNotify(
            @RequestBody String body,
            HttpServletRequest request) {
        PaymentGateway gateway = wechatGatewayProvider.getIfAvailable();
        if (gateway == null) {
            log.warn("WechatPaymentGateway not configured, ignoring notify");
            return ResponseEntity.ok().build();
        }

        // 提取微信签名相关头部
        Map<String, String> headers = extractWechatHeaders(request);
        log.debug("Received wechat notify, body length: {}", body.length());

        NotifyResult result = gateway.parseNotify(headers, body);
        eventPublisher.publishEvent(new PaymentNotifyEvent(this, result));
        log.info("Wechat notify published, outTradeNo: {}, success: {}",
                result.getOutTradeNo(), result.isSuccess());
        return ResponseEntity.ok().build();
    }

    /**
     * 从 HTTP 请求中提取表单参数（支持 URL 编码表单）。
     */
    private Map<String, String> extractFormParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                params.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        return Collections.unmodifiableMap(params);
    }

    /**
     * 提取微信支付回调所需的头部信息。
     */
    private Map<String, String> extractWechatHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Wechatpay-Serial", request.getHeader("Wechatpay-Serial"));
        headers.put("Wechatpay-Nonce", request.getHeader("Wechatpay-Nonce"));
        headers.put("Wechatpay-Signature", request.getHeader("Wechatpay-Signature"));
        headers.put("Wechatpay-Timestamp", request.getHeader("Wechatpay-Timestamp"));
        return Collections.unmodifiableMap(headers);
    }
}
