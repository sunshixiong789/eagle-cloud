package com.eagle.payment.core.infrastructure.gateway.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.eagle.payment.core.common.exception.PaymentErrorCode;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.port.GatewayNotifyResult;
import com.eagle.payment.core.domain.port.GatewayPayCommand;
import com.eagle.payment.core.domain.port.GatewayPayResult;
import com.eagle.payment.core.domain.port.GatewayQueryResult;
import com.eagle.payment.core.domain.port.MerchantResolverPort;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付宝渠道适配器,实现 {@link PaymentGatewayPort}。
 *
 * <p>支持场景与对应 API:
 * <ul>
 *   <li>{@link PaymentScene#PC_WEB}      - alipay.trade.page.pay (返回 HTML form)</li>
 *   <li>{@link PaymentScene#MOBILE_WEB}  - alipay.trade.wap.pay (返回 HTML form)</li>
 *   <li>{@link PaymentScene#APP}         - alipay.trade.app.pay (返回 orderInfo)</li>
 *   <li>{@link PaymentScene#NATIVE_QR}   - alipay.trade.precreate (返回 qrCode URL)</li>
 * </ul>
 *
 * <p>v1 单商户:在 {@link PostConstruct} 中根据 {@link MerchantResolverPort} 初始化
 * 单个 {@link AlipayClient};v2 多商户时改为 {@code ConcurrentHashMap<merchantKey, AlipayClient>}
 * 按需懒加载并自动失效。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "eagle.payment.alipay", name = "enabled", havingValue = "true")
public class AlipayPaymentGatewayAdapter implements PaymentGatewayPort {

    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    private static final String TRADE_FINISHED = "TRADE_FINISHED";
    private static final String TRADE_CLOSED = "TRADE_CLOSED";
    private static final String WAIT_BUYER_PAY = "WAIT_BUYER_PAY";

    private static final DateTimeFormatter EXPIRE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MerchantResolverPort merchantResolver;
    private final PaymentProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AlipayClient client;
    private String alipayPublicKey;
    private String signType;
    private String charset;
    private String notifyBaseUrl;

    public AlipayPaymentGatewayAdapter(MerchantResolverPort merchantResolver,
                                       PaymentProperties properties) {
        this.merchantResolver = merchantResolver;
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        // v1 单商户:用空 tenantId 取全局凭证
        Map<String, String> creds = merchantResolver.resolve("", PaymentChannel.ALIPAY);
        if (creds == null) {
            log.warn("Alipay enabled but credentials missing — gateway will reject all requests");
            return;
        }
        this.client = new DefaultAlipayClient(
                creds.get("gatewayUrl"),
                creds.get("appId"),
                creds.get("privateKey"),
                creds.get("format"),
                creds.get("charset"),
                creds.get("alipayPublicKey"),
                creds.get("signType")
        );
        this.alipayPublicKey = creds.get("alipayPublicKey");
        this.signType = creds.get("signType");
        this.charset = creds.get("charset");
        this.notifyBaseUrl = creds.get("notifyBaseUrl");
        log.info("Alipay gateway initialized, appId={}", creds.get("appId"));
    }

    @Override
    public PaymentChannel getChannel() {
        return PaymentChannel.ALIPAY;
    }

    @Override
    public GatewayPayResult createPayment(GatewayPayCommand command) {
        ensureReady();
        return switch (command.scene()) {
            case PC_WEB -> pagePay(command);
            case MOBILE_WEB -> wapPay(command);
            case APP -> appPay(command);
            case NATIVE_QR -> precreate(command);
            case JSAPI, MINI_PROGRAM -> throw PaymentErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        };
    }

    private GatewayPayResult pagePay(GatewayPayCommand command) {
        try {
            AlipayTradePagePayRequest req = new AlipayTradePagePayRequest();
            applyCommon(req, command);
            req.setBizContent(toBizContentForBrowserPay(command, "FAST_INSTANT_TRADE_PAY"));
            AlipayTradePagePayResponse resp = client.pageExecute(req);
            if (!resp.isSuccess()) {
                throw gatewayException(resp.getSubCode(), resp.getSubMsg());
            }
            return new GatewayPayResult(null, resp.getBody(), "html-form");
        } catch (AlipayApiException | JacksonException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    private GatewayPayResult wapPay(GatewayPayCommand command) {
        try {
            AlipayTradeWapPayRequest req = new AlipayTradeWapPayRequest();
            applyCommon(req, command);
            req.setBizContent(toBizContentForBrowserPay(command, "QUICK_WAP_WAY"));
            AlipayTradeWapPayResponse resp = client.pageExecute(req);
            if (!resp.isSuccess()) {
                throw gatewayException(resp.getSubCode(), resp.getSubMsg());
            }
            return new GatewayPayResult(null, resp.getBody(), "html-form");
        } catch (AlipayApiException | JacksonException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    private GatewayPayResult appPay(GatewayPayCommand command) {
        try {
            AlipayTradeAppPayRequest req = new AlipayTradeAppPayRequest();
            req.setNotifyUrl(buildNotifyUrl());
            req.setBizContent(toBizContentForBrowserPay(command, "QUICK_MSECURITY_PAY"));
            AlipayTradeAppPayResponse resp = client.sdkExecute(req);
            if (!resp.isSuccess()) {
                throw gatewayException(resp.getSubCode(), resp.getSubMsg());
            }
            return new GatewayPayResult(null, resp.getBody(), "order-info");
        } catch (AlipayApiException | JacksonException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    private GatewayPayResult precreate(GatewayPayCommand command) {
        try {
            AlipayTradePrecreateRequest req = new AlipayTradePrecreateRequest();
            req.setNotifyUrl(buildNotifyUrl());
            Map<String, Object> biz = baseBizContent(command);
            req.setBizContent(objectMapper.writeValueAsString(biz));
            AlipayTradePrecreateResponse resp = client.execute(req);
            if (!resp.isSuccess()) {
                throw gatewayException(resp.getSubCode(), resp.getSubMsg());
            }
            return new GatewayPayResult(null, resp.getQrCode(), "qr-code");
        } catch (AlipayApiException | JacksonException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayQueryResult queryPayment(PaymentChannel channel, String outTradeNo) {
        ensureReady();
        try {
            AlipayTradeQueryRequest req = new AlipayTradeQueryRequest();
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("out_trade_no", outTradeNo);
            req.setBizContent(objectMapper.writeValueAsString(biz));
            AlipayTradeQueryResponse resp = client.execute(req);
            if (!resp.isSuccess()) {
                log.warn("alipay query failed, outTradeNo={}, sub={}/{}",
                        outTradeNo, resp.getSubCode(), resp.getSubMsg());
                return new GatewayQueryResult(null, PaymentStatus.PAYING, null, null,
                        resp.getSubMsg());
            }
            PaymentStatus status = mapTradeStatus(resp.getTradeStatus());
            BigDecimal amount = resp.getTotalAmount() != null
                    ? new BigDecimal(resp.getTotalAmount()) : null;
            return new GatewayQueryResult(resp.getTradeNo(), status, amount, null, null);
        } catch (AlipayApiException | JacksonException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayNotifyResult parseNotify(Map<String, String> headers, String rawBody,
                                           Map<String, String> formParams) {
        try {
            boolean valid = AlipaySignature.rsaCheckV1(formParams, alipayPublicKey, charset, signType);
            if (!valid) {
                return GatewayNotifyResult.invalid(rawBody);
            }
        } catch (AlipayApiException e) {
            log.warn("alipay notify rsaCheckV1 exception", e);
            return GatewayNotifyResult.invalid(rawBody);
        }
        String tradeStatus = formParams.get("trade_status");
        PaymentStatus status = mapTradeStatus(tradeStatus);
        BigDecimal amount = formParams.get("total_amount") != null
                ? new BigDecimal(formParams.get("total_amount")) : null;
        LocalDateTime paidAt = parseGmtPayment(formParams.get("gmt_payment"));
        // 支付宝以 notify_id 作为去重键
        String eventId = formParams.get("notify_id");
        return new GatewayNotifyResult(
                true,
                formParams.get("out_trade_no"),
                formParams.get("trade_no"),
                status,
                amount,
                paidAt,
                TRADE_CLOSED.equals(tradeStatus) ? "trade closed" : null,
                rawBody,
                eventId
        );
    }

    private void ensureReady() {
        if (client == null) {
            throw PaymentErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
    }

    /** 浏览器拉起支付场景的通用 bizContent。 */
    private String toBizContentForBrowserPay(GatewayPayCommand command, String productCode)
            throws JacksonException {
        Map<String, Object> biz = baseBizContent(command);
        biz.put("product_code", productCode);
        return objectMapper.writeValueAsString(biz);
    }

    private Map<String, Object> baseBizContent(GatewayPayCommand command) {
        Map<String, Object> biz = new LinkedHashMap<>();
        biz.put("out_trade_no", command.outTradeNo());
        biz.put("total_amount", command.amount().setScale(2).toPlainString());
        biz.put("subject", command.subject());
        if (command.expiresAt() != null) {
            biz.put("time_expire", command.expiresAt().format(EXPIRE_FORMATTER));
        }
        return biz;
    }

    private void applyCommon(com.alipay.api.AlipayRequest<?> req, GatewayPayCommand command) {
        // pagePay / wapPay 走 pageExecute,需要 returnUrl + notifyUrl
        if (req instanceof AlipayTradePagePayRequest p) {
            p.setNotifyUrl(buildNotifyUrl());
            if (command.returnUrl() != null) {
                p.setReturnUrl(command.returnUrl());
            }
        } else if (req instanceof AlipayTradeWapPayRequest w) {
            w.setNotifyUrl(buildNotifyUrl());
            if (command.returnUrl() != null) {
                w.setReturnUrl(command.returnUrl());
            }
        }
    }

    private String buildNotifyUrl() {
        if (notifyBaseUrl == null || notifyBaseUrl.isEmpty()) {
            return "";
        }
        return notifyBaseUrl + "/payment/alipay/notify";
    }

    private RuntimeException gatewayException(String subCode, String subMsg) {
        log.warn("alipay gateway error, subCode={}, subMsg={}", subCode, subMsg);
        return PaymentErrorCode.GATEWAY_ERROR.toServiceException();
    }

    private PaymentStatus mapTradeStatus(@Nullable String tradeStatus) {
        if (tradeStatus == null) {
            return PaymentStatus.PAYING;
        }
        return switch (tradeStatus) {
            case TRADE_SUCCESS, TRADE_FINISHED -> PaymentStatus.PAID;
            case TRADE_CLOSED -> PaymentStatus.FAILED;
            case WAIT_BUYER_PAY -> PaymentStatus.PAYING;
            default -> PaymentStatus.PAYING;
        };
    }

    @Nullable
    private LocalDateTime parseGmtPayment(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw, EXPIRE_FORMATTER);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // 暂未使用,保留方法签名以供后续 RefundService 使用
    @SuppressWarnings("unused")
    private Map<String, Object> reservedRefundBizContent(String outTradeNo, BigDecimal amount,
                                                         String refundNo) {
        Map<String, Object> biz = new HashMap<>();
        biz.put("out_trade_no", outTradeNo);
        biz.put("refund_amount", amount.setScale(2).toPlainString());
        biz.put("out_request_no", refundNo);
        return biz;
    }
}
