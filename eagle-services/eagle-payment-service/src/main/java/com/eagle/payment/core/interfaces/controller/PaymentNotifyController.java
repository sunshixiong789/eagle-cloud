package com.eagle.payment.core.interfaces.controller;

import com.eagle.payment.core.application.service.PaymentNotifyApplicationService;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.port.GatewayNotifyResult;
import com.eagle.payment.core.domain.port.GatewayRefundNotifyResult;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 支付渠道异步通知接收 (公网 POST 入口)。
 *
 * <p>放行规则: 见 {@code application.yml} {@code eagle.resource-server.permit-paths}。
 * 生产环境必须额外配合网关层 IP 白名单 (支付宝 / 微信回调 IP 段) + Sentinel 限流。
 *
 * <p>回调路径划分:
 * <ul>
 *   <li>{@code POST /payment/alipay/notify}        - 支付完成 + 退款完成 (按 refund_fee 字段分流)</li>
 *   <li>{@code POST /payment/wechat/notify}        - 微信支付完成</li>
 *   <li>{@code POST /payment/wechat/refund-notify} - 微信退款完成 (独立 URL,渠道侧设置)</li>
 * </ul>
 *
 * <p>响应规范:
 * <ul>
 *   <li>支付宝 - 验签 OK 且业务处理完返回 "success",其他一律 "fail" (含验签失败 / 业务异常)</li>
 *   <li>微信   - 200 OK 表示已接收;返回非 200 会触发渠道重投递</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Tag(name = "支付回调 (公网)", description = "渠道异步通知入口,需配合网关 IP 白名单防护")
@RestController
@RequestMapping("/payment")
public class PaymentNotifyController {

    private final PaymentNotifyApplicationService notifyApplicationService;
    private final Map<PaymentChannel, PaymentGatewayPort> gateways;

    public PaymentNotifyController(PaymentNotifyApplicationService notifyApplicationService,
                                   List<PaymentGatewayPort> gatewayPorts) {
        this.notifyApplicationService = notifyApplicationService;
        Map<PaymentChannel, PaymentGatewayPort> map = new EnumMap<>(PaymentChannel.class);
        for (PaymentGatewayPort port : gatewayPorts) {
            map.put(port.getChannel(), port);
        }
        this.gateways = map;
    }

    @Operation(summary = "支付宝异步回调 (支付 + 退款共用)",
            description = "rsaCheckV1 验签;按 refund_fee 字段分流到支付完成 / 退款完成处理")
    @PostMapping("/alipay/notify")
    public ResponseEntity<String> alipayNotify(HttpServletRequest request) {
        PaymentGatewayPort gateway = gateways.get(PaymentChannel.ALIPAY);
        if (gateway == null) {
            log.warn("alipay notify received but gateway not configured");
            return ResponseEntity.ok("fail");
        }
        Map<String, String> formParams = extractFormParams(request);
        Map<String, String> headers = extractHeaders(request);
        boolean isRefundNotify = formParams.containsKey("refund_fee")
                || formParams.containsKey("out_biz_no");
        if (isRefundNotify) {
            GatewayRefundNotifyResult refundResult =
                    gateway.parseRefundNotify(headers, null, formParams);
            var outcome = notifyApplicationService.handleRefund(PaymentChannel.ALIPAY, refundResult);
            log.info("alipay refund notify processed, outcome={}, refundNo={}",
                    outcome, refundResult.refundNo());
            return ResponseEntity.ok(outcome.isAck() ? "success" : "fail");
        }
        GatewayNotifyResult result = gateway.parseNotify(headers, null, formParams);
        var outcome = notifyApplicationService.handle(PaymentChannel.ALIPAY, result);
        log.info("alipay payment notify processed, outcome={}, outTradeNo={}",
                outcome, result.outTradeNo());
        return ResponseEntity.ok(outcome.isAck() ? "success" : "fail");
    }

    @Operation(summary = "微信支付异步回调",
            description = "wechatpay-java NotificationParser 验签;200 = 已接收,非 200 触发重投")
    @PostMapping("/wechat/notify")
    public ResponseEntity<Void> wechatNotify(@RequestBody String body, HttpServletRequest request) {
        PaymentGatewayPort gateway = gateways.get(PaymentChannel.WECHAT);
        if (gateway == null) {
            log.warn("wechat notify received but gateway not configured");
            return ResponseEntity.status(500).build();
        }
        Map<String, String> headers = extractHeaders(request);
        GatewayNotifyResult result = gateway.parseNotify(headers, body, Collections.emptyMap());
        var outcome = notifyApplicationService.handle(PaymentChannel.WECHAT, result);
        log.info("wechat payment notify processed, outcome={}, outTradeNo={}",
                outcome, result.outTradeNo());
        return ResponseEntity.status(outcome.isAck() ? 200 : 500).build();
    }

    @Operation(summary = "微信退款异步回调",
            description = "wechatpay-java NotificationParser 解析 RefundNotification;独立 URL")
    @PostMapping("/wechat/refund-notify")
    public ResponseEntity<Void> wechatRefundNotify(@RequestBody String body,
                                                   HttpServletRequest request) {
        PaymentGatewayPort gateway = gateways.get(PaymentChannel.WECHAT);
        if (gateway == null) {
            log.warn("wechat refund notify received but gateway not configured");
            return ResponseEntity.status(500).build();
        }
        Map<String, String> headers = extractHeaders(request);
        GatewayRefundNotifyResult result =
                gateway.parseRefundNotify(headers, body, Collections.emptyMap());
        var outcome = notifyApplicationService.handleRefund(PaymentChannel.WECHAT, result);
        log.info("wechat refund notify processed, outcome={}, refundNo={}",
                outcome, result.refundNo());
        return ResponseEntity.status(outcome.isAck() ? 200 : 500).build();
    }

    private Map<String, String> extractFormParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), values[0]);
            }
        }
        return Collections.unmodifiableMap(params);
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return Collections.unmodifiableMap(headers);
    }
}
