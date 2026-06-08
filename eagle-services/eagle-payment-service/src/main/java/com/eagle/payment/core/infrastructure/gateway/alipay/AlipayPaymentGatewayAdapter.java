package com.eagle.payment.core.infrastructure.gateway.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayFundTransUniTransferRequest;
import com.alipay.api.request.AlipayFundTransCommonQueryRequest;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayFundTransUniTransferResponse;
import com.alipay.api.response.AlipayFundTransCommonQueryResponse;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.eagle.payment.core.common.exception.PaymentErrorCode;
import com.eagle.payment.core.common.exception.RefundErrorCode;
import com.eagle.payment.core.common.exception.TransferErrorCode;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.model.enums.RefundStatus;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import com.eagle.payment.core.domain.port.GatewayNotifyResult;
import com.eagle.payment.core.domain.port.GatewayPayCommand;
import com.eagle.payment.core.domain.port.GatewayPayResult;
import com.eagle.payment.core.domain.port.GatewayQueryResult;
import com.eagle.payment.core.domain.port.GatewayRefundCommand;
import com.eagle.payment.core.domain.port.GatewayRefundNotifyResult;
import com.eagle.payment.core.domain.port.GatewayRefundResult;
import com.eagle.payment.core.domain.port.GatewayTransferCommand;
import com.eagle.payment.core.domain.port.GatewayTransferResult;
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
        Map<String, String> creds = merchantResolver.resolve(PaymentChannel.ALIPAY);
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
    public GatewayRefundResult refund(GatewayRefundCommand command) {
        ensureReady();
        try {
            AlipayTradeRefundRequest req = new AlipayTradeRefundRequest();
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("out_trade_no", command.paymentOutTradeNo());
            biz.put("refund_amount", command.refundAmount().setScale(2).toPlainString());
            biz.put("out_request_no", command.refundNo());
            if (command.reason() != null) {
                biz.put("refund_reason", command.reason());
            }
            req.setBizContent(objectMapper.writeValueAsString(biz));
            AlipayTradeRefundResponse resp = client.execute(req);
            if (!resp.isSuccess()) {
                log.warn("alipay refund failed, refundNo={}, sub={}/{}",
                        command.refundNo(), resp.getSubCode(), resp.getSubMsg());
                return new GatewayRefundResult(null, RefundStatus.FAILED, null,
                        resp.getSubMsg());
            }
            // 支付宝 refund 同步即返回最终结果;trade_no 为渠道侧交易号 (复用同一笔交易号)
            return new GatewayRefundResult(
                    resp.getTradeNo(), RefundStatus.REFUNDED, LocalDateTime.now(), null);
        } catch (AlipayApiException | JacksonException e) {
            throw RefundErrorCode.REFUND_GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayRefundResult queryRefund(PaymentChannel channel, String refundNo) {
        ensureReady();
        try {
            AlipayTradeFastpayRefundQueryRequest req = new AlipayTradeFastpayRefundQueryRequest();
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("out_request_no", refundNo);
            // 支付宝查询 refund 需要原 out_trade_no,这里调用方传 refundNo 单独无法定位;
            // 但 v0.2 协议允许仅传 out_request_no 时由 out_trade_no 复用 (调用方在
            // 上层会同时传入两者,此 port 由 ApplicationService 在调用时填齐)。
            // 为保持端口签名简洁,此方法在 P0-2 单作"未提供原 trade_no 时返回 PROCESSING"。
            req.setBizContent(objectMapper.writeValueAsString(biz));
            AlipayTradeFastpayRefundQueryResponse resp = client.execute(req);
            if (!resp.isSuccess() || resp.getRefundStatus() == null) {
                return new GatewayRefundResult(null, RefundStatus.REFUNDING, null, resp.getSubMsg());
            }
            // refund_status: REFUND_SUCCESS / REFUND_FAIL
            RefundStatus status = "REFUND_SUCCESS".equals(resp.getRefundStatus())
                    ? RefundStatus.REFUNDED : RefundStatus.FAILED;
            return new GatewayRefundResult(resp.getTradeNo(), status,
                    status == RefundStatus.REFUNDED ? LocalDateTime.now() : null,
                    status == RefundStatus.FAILED ? "channel reported FAIL" : null);
        } catch (AlipayApiException | JacksonException e) {
            throw RefundErrorCode.REFUND_GATEWAY_ERROR.toServiceException(e);
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

    @Override
    public GatewayRefundNotifyResult parseRefundNotify(Map<String, String> headers, String rawBody,
                                                       Map<String, String> formParams) {
        // 支付宝退款回调与支付回调走同一 notify_url;通过 trade_status / refund_fee 字段区分。
        // 调用方应在 Controller 层先用 parseNotify 验签 + 取关键字段,再按 refund_fee != null
        // 判断为退款回调。这里仅在已识别为退款回调时被调用,主要做信号映射。
        try {
            boolean valid = AlipaySignature.rsaCheckV1(formParams, alipayPublicKey, charset, signType);
            if (!valid) {
                return GatewayRefundNotifyResult.invalid(rawBody);
            }
        } catch (AlipayApiException e) {
            log.warn("alipay refund notify rsaCheckV1 exception", e);
            return GatewayRefundNotifyResult.invalid(rawBody);
        }
        // 支付宝退款异步通知不一定包含独立的 refund_status;TRADE_SUCCESS + refund_fee
        // 通常意味着部分退或全退已成功落账。完整状态机一般通过同步退款响应已确定,
        // 异步通知只是补单兜底。这里返回 REFUNDED + amount。
        String refundNo = formParams.get("out_biz_no");
        if (refundNo == null) {
            refundNo = formParams.get("out_request_no");
        }
        String refundFee = formParams.get("refund_fee");
        BigDecimal amount = refundFee != null ? new BigDecimal(refundFee) : null;
        return new GatewayRefundNotifyResult(
                true,
                refundNo,
                formParams.get("trade_no"),
                RefundStatus.REFUNDED,
                amount,
                parseGmtPayment(formParams.get("gmt_refund")),
                null,
                rawBody
        );
    }

    @Override
    public GatewayTransferResult transfer(GatewayTransferCommand command) {
        ensureReady();
        try {
            AlipayFundTransUniTransferRequest req = new AlipayFundTransUniTransferRequest();
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("out_biz_no", command.transferNo());
            biz.put("trans_amount", command.amount().setScale(2).toPlainString());
            biz.put("product_code", "TRANS_ACCOUNT_NO_PWD");
            biz.put("biz_scene", "DIRECT_TRANSFER");
            if (command.reason() != null) {
                biz.put("remark", command.reason());
            }
            Map<String, Object> payee = new LinkedHashMap<>();
            payee.put("identity", command.recipientAccount());
            payee.put("identity_type", "ALIPAY_LOGON_ID");
            if (command.recipientName() != null) {
                payee.put("name", command.recipientName());
            }
            biz.put("payee_info", payee);

            req.setBizContent(objectMapper.writeValueAsString(biz));
            AlipayFundTransUniTransferResponse resp = client.execute(req);
            if (!resp.isSuccess()) {
                log.warn("alipay transfer failed, transferNo={}, sub={}/{}",
                        command.transferNo(), resp.getSubCode(), resp.getSubMsg());
                return new GatewayTransferResult(null, TransferStatus.FAILED, null,
                        resp.getSubMsg());
            }
            // 支付宝转账成功同步返回 order_id;status 字段不一定有,以 isSuccess() + 业务规范认为 SUCCESS
            return new GatewayTransferResult(resp.getOrderId(), TransferStatus.SUCCESS,
                    LocalDateTime.now(), null);
        } catch (AlipayApiException | JacksonException e) {
            throw TransferErrorCode.TRANSFER_GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayTransferResult queryTransfer(PaymentChannel channel, String transferNo) {
        ensureReady();
        try {
            AlipayFundTransCommonQueryRequest req = new AlipayFundTransCommonQueryRequest();
            Map<String, Object> biz = new LinkedHashMap<>();
            biz.put("product_code", "TRANS_ACCOUNT_NO_PWD");
            biz.put("biz_scene", "DIRECT_TRANSFER");
            biz.put("out_biz_no", transferNo);
            req.setBizContent(objectMapper.writeValueAsString(biz));
            AlipayFundTransCommonQueryResponse resp = client.execute(req);
            if (!resp.isSuccess()) {
                return new GatewayTransferResult(null, TransferStatus.SUBMITTED, null,
                        resp.getSubMsg());
            }
            // status: SUCCESS / FAIL / REFUND / DEALING / WAIT_PAY
            TransferStatus status = switch (String.valueOf(resp.getStatus())) {
                case "SUCCESS" -> TransferStatus.SUCCESS;
                case "FAIL" -> TransferStatus.FAILED;
                case "REFUND" -> TransferStatus.RETURNED;
                default -> TransferStatus.SUBMITTED;
            };
            return new GatewayTransferResult(resp.getOrderId(), status,
                    status == TransferStatus.SUCCESS ? LocalDateTime.now() : null,
                    status == TransferStatus.FAILED ? "channel reported FAIL" : null);
        } catch (AlipayApiException | JacksonException e) {
            throw TransferErrorCode.TRANSFER_GATEWAY_ERROR.toServiceException(e);
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
