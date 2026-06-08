package com.eagle.payment.core.infrastructure.gateway.wechat;

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
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import com.wechat.pay.java.service.transferbatch.TransferBatchService;
import com.wechat.pay.java.service.transferbatch.model.GetTransferBatchByOutNoRequest;
import com.wechat.pay.java.service.transferbatch.model.InitiateBatchTransferRequest;
import com.wechat.pay.java.service.transferbatch.model.InitiateBatchTransferResponse;
import com.wechat.pay.java.service.transferbatch.model.TransferBatchEntity;
import com.wechat.pay.java.service.transferbatch.model.TransferBatchGet;
import com.wechat.pay.java.service.transferbatch.model.TransferDetailInput;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.AutoCertificateNotificationConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.app.AppService;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.h5.model.H5Info;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 微信支付渠道适配器 (V3 API,使用 wechatpay-java SDK 0.2.x)。
 *
 * <p>支持场景与对应 API:
 * <ul>
 *   <li>{@link PaymentScene#NATIVE_QR}      - /v3/pay/transactions/native (扫码,返回 codeUrl)</li>
 *   <li>{@link PaymentScene#JSAPI}          - /v3/pay/transactions/jsapi (公众号/小程序,返回 prepayId)</li>
 *   <li>{@link PaymentScene#MINI_PROGRAM}   - 同 JSAPI (V3 共用同一接口,场景区分由 appid 决定)</li>
 *   <li>{@link PaymentScene#APP}            - /v3/pay/transactions/app (APP,返回 prepayId)</li>
 *   <li>{@link PaymentScene#MOBILE_WEB}     - /v3/pay/transactions/h5 (H5,返回 mwebUrl)</li>
 * </ul>
 *
 * <p>支付宝 PC_WEB 场景在微信侧无直接对应 (NATIVE_QR 已覆盖扫码场景),提交即抛
 * {@link PaymentErrorCode#CHANNEL_UNAVAILABLE}。
 *
 * <p>异步通知验签走 {@link NotificationParser}+ V3 平台证书自动轮换 (
 * {@link RSAAutoCertificateConfig}),无需手动管理证书过期。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "eagle.payment.wechat", name = "enabled", havingValue = "true")
public class WechatPaymentGatewayAdapter implements PaymentGatewayPort {

    private final MerchantResolverPort merchantResolver;
    private final PaymentProperties properties;
    private String appId;
    private String notifyBaseUrl;
    private Config config;
    private NotificationConfig notificationConfig;
    private NativePayService nativeService;
    private JsapiService jsapiService;
    private AppService appService;
    private H5Service h5Service;
    private RefundService refundService;
    private TransferBatchService transferBatchService;
    private NotificationParser notificationParser;

    public WechatPaymentGatewayAdapter(MerchantResolverPort merchantResolver,
                                       PaymentProperties properties) {
        this.merchantResolver = merchantResolver;
        this.properties = properties;
    }

    @SuppressWarnings("deprecation")
    @PostConstruct
    void init() {
        Map<String, String> creds = merchantResolver.resolve(PaymentChannel.WECHAT);
        if (creds == null) {
            log.warn("WeChat Pay enabled but credentials missing — gateway will reject all requests");
            return;
        }
        this.appId = creds.get("appId");
        this.notifyBaseUrl = creds.get("notifyBaseUrl");
        this.config = new RSAAutoCertificateConfig.Builder()
                .merchantId(creds.get("mchId"))
                .privateKey(creds.get("privateKey"))
                .merchantSerialNumber(creds.get("privateKeySerialNo"))
                .apiV3Key(creds.get("apiV3Key"))
                .build();
        this.notificationConfig = new AutoCertificateNotificationConfig.Builder()
                .merchantId(creds.get("mchId"))
                .privateKey(creds.get("privateKey"))
                .merchantSerialNumber(creds.get("privateKeySerialNo"))
                .apiV3Key(creds.get("apiV3Key"))
                .build();
        this.notificationParser = new NotificationParser(this.notificationConfig);
        this.nativeService = new NativePayService.Builder().config(config).build();
        this.jsapiService = new JsapiService.Builder().config(config).build();
        this.appService = new AppService.Builder().config(config).build();
        this.h5Service = new H5Service.Builder().config(config).build();
        this.refundService = new RefundService.Builder().config(config).build();
        this.transferBatchService = new TransferBatchService.Builder().config(config).build();
        log.info("WeChat Pay gateway initialized, mchId={}", creds.get("mchId"));
    }

    @Override
    public PaymentChannel getChannel() {
        return PaymentChannel.WECHAT;
    }

    @Override
    public GatewayPayResult createPayment(GatewayPayCommand command) {
        ensureReady();
        return switch (command.scene()) {
            case NATIVE_QR -> prepayNative(command);
            case JSAPI, MINI_PROGRAM -> prepayJsapi(command);
            case APP -> prepayApp(command);
            case MOBILE_WEB -> prepayH5(command);
            case PC_WEB -> throw PaymentErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        };
    }

    private GatewayPayResult prepayNative(GatewayPayCommand command) {
        var req = new com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest();
        req.setAppid(appId);
        req.setMchid(properties.getWechat().getMchId());
        req.setDescription(command.subject());
        req.setOutTradeNo(command.outTradeNo());
        req.setNotifyUrl(buildNotifyUrl());
        if (command.expiresAt() != null) {
            req.setTimeExpire(toIso8601(command.expiresAt()));
        }
        var amount = new com.wechat.pay.java.service.payments.nativepay.model.Amount();
        amount.setTotal(toYuanCents(command.amount()));
        amount.setCurrency(command.currency() == null ? "CNY" : command.currency());
        req.setAmount(amount);
        try {
            var resp = nativeService.prepay(req);
            return new GatewayPayResult(null, resp.getCodeUrl(), "code-url");
        } catch (RuntimeException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    private GatewayPayResult prepayJsapi(GatewayPayCommand command) {
        if (command.openId() == null || command.openId().isEmpty()) {
            throw PaymentErrorCode.INVALID_STATUS.toDomainException();
        }
        var req = new com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest();
        req.setAppid(appId);
        req.setMchid(properties.getWechat().getMchId());
        req.setDescription(command.subject());
        req.setOutTradeNo(command.outTradeNo());
        req.setNotifyUrl(buildNotifyUrl());
        if (command.expiresAt() != null) {
            req.setTimeExpire(toIso8601(command.expiresAt()));
        }
        var amount = new com.wechat.pay.java.service.payments.jsapi.model.Amount();
        amount.setTotal(toYuanCents(command.amount()));
        amount.setCurrency(command.currency() == null ? "CNY" : command.currency());
        req.setAmount(amount);
        Payer payer = new Payer();
        payer.setOpenid(command.openId());
        req.setPayer(payer);
        try {
            var resp = jsapiService.prepay(req);
            return new GatewayPayResult(null, resp.getPrepayId(), "prepay-id");
        } catch (RuntimeException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    private GatewayPayResult prepayApp(GatewayPayCommand command) {
        var req = new com.wechat.pay.java.service.payments.app.model.PrepayRequest();
        req.setAppid(appId);
        req.setMchid(properties.getWechat().getMchId());
        req.setDescription(command.subject());
        req.setOutTradeNo(command.outTradeNo());
        req.setNotifyUrl(buildNotifyUrl());
        if (command.expiresAt() != null) {
            req.setTimeExpire(toIso8601(command.expiresAt()));
        }
        var amount = new com.wechat.pay.java.service.payments.app.model.Amount();
        amount.setTotal(toYuanCents(command.amount()));
        amount.setCurrency(command.currency() == null ? "CNY" : command.currency());
        req.setAmount(amount);
        try {
            var resp = appService.prepay(req);
            return new GatewayPayResult(null, resp.getPrepayId(), "prepay-id");
        } catch (RuntimeException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    private GatewayPayResult prepayH5(GatewayPayCommand command) {
        var req = new com.wechat.pay.java.service.payments.h5.model.PrepayRequest();
        req.setAppid(appId);
        req.setMchid(properties.getWechat().getMchId());
        req.setDescription(command.subject());
        req.setOutTradeNo(command.outTradeNo());
        req.setNotifyUrl(buildNotifyUrl());
        if (command.expiresAt() != null) {
            req.setTimeExpire(toIso8601(command.expiresAt()));
        }
        var amount = new com.wechat.pay.java.service.payments.h5.model.Amount();
        amount.setTotal(toYuanCents(command.amount()));
        amount.setCurrency(command.currency() == null ? "CNY" : command.currency());
        req.setAmount(amount);

        var sceneInfo = new com.wechat.pay.java.service.payments.h5.model.SceneInfo();
        sceneInfo.setPayerClientIp(command.clientIp() == null ? "127.0.0.1" : command.clientIp());
        H5Info h5Info = new H5Info();
        h5Info.setType("Wap");
        sceneInfo.setH5Info(h5Info);
        req.setSceneInfo(sceneInfo);

        try {
            var resp = h5Service.prepay(req);
            return new GatewayPayResult(null, resp.getH5Url(), "h5-url");
        } catch (RuntimeException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayQueryResult queryPayment(PaymentChannel channel, String outTradeNo) {
        ensureReady();
        try {
            var req = new com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest();
            req.setOutTradeNo(outTradeNo);
            req.setMchid(properties.getWechat().getMchId());
            Transaction tx = nativeService.queryOrderByOutTradeNo(req);
            return mapTransaction(tx);
        } catch (RuntimeException e) {
            throw PaymentErrorCode.GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayNotifyResult parseNotify(Map<String, String> headers, String rawBody,
                                           Map<String, String> formParams) {
        try {
            RequestParam param = new RequestParam.Builder()
                    .serialNumber(headers.get("Wechatpay-Serial"))
                    .nonce(headers.get("Wechatpay-Nonce"))
                    .signature(headers.get("Wechatpay-Signature"))
                    .timestamp(headers.get("Wechatpay-Timestamp"))
                    .body(rawBody)
                    .build();
            Transaction tx = notificationParser.parse(param, Transaction.class);
            PaymentStatus status = mapTradeState(tx.getTradeState() == null
                    ? null : tx.getTradeState().name());
            BigDecimal amount = tx.getAmount() == null || tx.getAmount().getTotal() == null
                    ? null
                    : BigDecimal.valueOf(tx.getAmount().getTotal()).movePointLeft(2);
            LocalDateTime paidAt = parseSuccessTime(tx.getSuccessTime());
            return new GatewayNotifyResult(
                    true,
                    tx.getOutTradeNo(),
                    tx.getTransactionId(),
                    status,
                    amount,
                    paidAt,
                    status == PaymentStatus.FAILED ? tx.getTradeStateDesc() : null,
                    rawBody,
                    // 微信 V3 通知含独立的 id 字段,但 NotificationParser 内部消费;退而使用
                    // out_trade_no + tradeState + transactionId 联合作为去重键,在 ApplicationService 侧 hash
                    null
            );
        } catch (RuntimeException e) {
            log.warn("wechat notify parse / verify failed", e);
            return GatewayNotifyResult.invalid(rawBody);
        }
    }

    private GatewayQueryResult mapTransaction(Transaction tx) {
        PaymentStatus status = mapTradeState(tx.getTradeState() == null
                ? null : tx.getTradeState().name());
        BigDecimal amount = tx.getAmount() == null || tx.getAmount().getTotal() == null
                ? null
                : BigDecimal.valueOf(tx.getAmount().getTotal()).movePointLeft(2);
        return new GatewayQueryResult(
                tx.getTransactionId(),
                status,
                amount,
                parseSuccessTime(tx.getSuccessTime()),
                status == PaymentStatus.FAILED ? tx.getTradeStateDesc() : null
        );
    }

    private PaymentStatus mapTradeState(@Nullable String tradeState) {
        if (tradeState == null) {
            return PaymentStatus.PAYING;
        }
        return switch (tradeState) {
            case "SUCCESS" -> PaymentStatus.PAID;
            case "REFUND" -> PaymentStatus.PAID; // 退款维度由 Refund 域承载,Payment 仍是 PAID
            case "NOTPAY", "USERPAYING" -> PaymentStatus.PAYING;
            case "CLOSED", "REVOKED", "PAYERROR" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PAYING;
        };
    }

    private int toYuanCents(BigDecimal amountYuan) {
        // 微信侧单位为分,且为整型;BigDecimal 元 → 整数分
        return amountYuan.movePointRight(2).intValueExact();
    }

    private String buildNotifyUrl() {
        if (notifyBaseUrl == null || notifyBaseUrl.isEmpty()) {
            return "";
        }
        return notifyBaseUrl + "/payment/wechat/notify";
    }

    private String toIso8601(LocalDateTime ldt) {
        // 微信要求 ISO-8601 + 时区,例 2026-06-30T15:00:00+08:00
        return ldt.atZone(ZoneId.systemDefault())
                .toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Nullable
    private LocalDateTime parseSuccessTime(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (RuntimeException ignore) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(raw), ZoneId.systemDefault());
            } catch (RuntimeException ex) {
                return null;
            }
        }
    }

    @Override
    public GatewayRefundResult refund(GatewayRefundCommand command) {
        ensureReady();
        CreateRequest req = new CreateRequest();
        req.setOutTradeNo(command.paymentOutTradeNo());
        req.setOutRefundNo(command.refundNo());
        if (command.reason() != null) {
            req.setReason(command.reason());
        }
        req.setNotifyUrl(buildRefundNotifyUrl());
        AmountReq amount = new AmountReq();
        amount.setRefund(toLongCents(command.refundAmount()));
        amount.setTotal(toLongCents(command.originalAmount()));
        amount.setCurrency(command.currency() == null ? "CNY" : command.currency());
        req.setAmount(amount);
        try {
            com.wechat.pay.java.service.refund.model.Refund resp = refundService.create(req);
            RefundStatus status = mapRefundStatus(resp.getStatus());
            return new GatewayRefundResult(
                    resp.getRefundId(),
                    status,
                    parseSuccessTime(resp.getSuccessTime()),
                    status == RefundStatus.FAILED ? resp.getStatus().name() : null
            );
        } catch (RuntimeException e) {
            throw RefundErrorCode.REFUND_GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayRefundResult queryRefund(PaymentChannel channel, String refundNo) {
        ensureReady();
        try {
            QueryByOutRefundNoRequest req = new QueryByOutRefundNoRequest();
            req.setOutRefundNo(refundNo);
            com.wechat.pay.java.service.refund.model.Refund resp =
                    refundService.queryByOutRefundNo(req);
            RefundStatus status = mapRefundStatus(resp.getStatus());
            return new GatewayRefundResult(
                    resp.getRefundId(),
                    status,
                    parseSuccessTime(resp.getSuccessTime()),
                    status == RefundStatus.FAILED ? resp.getStatus().name() : null
            );
        } catch (RuntimeException e) {
            throw RefundErrorCode.REFUND_GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayRefundNotifyResult parseRefundNotify(Map<String, String> headers, String rawBody,
                                                       Map<String, String> formParams) {
        try {
            RequestParam param = new RequestParam.Builder()
                    .serialNumber(headers.get("Wechatpay-Serial"))
                    .nonce(headers.get("Wechatpay-Nonce"))
                    .signature(headers.get("Wechatpay-Signature"))
                    .timestamp(headers.get("Wechatpay-Timestamp"))
                    .body(rawBody)
                    .build();
            RefundNotification notif = notificationParser.parse(param, RefundNotification.class);
            RefundStatus status = mapRefundNotificationStatus(notif.getRefundStatus());
            BigDecimal amount = notif.getAmount() == null || notif.getAmount().getRefund() == null
                    ? null
                    : BigDecimal.valueOf(notif.getAmount().getRefund()).movePointLeft(2);
            return new GatewayRefundNotifyResult(
                    true,
                    notif.getOutRefundNo(),
                    notif.getRefundId(),
                    status,
                    amount,
                    parseSuccessTime(notif.getSuccessTime()),
                    status == RefundStatus.FAILED ? "channel reported FAIL" : null,
                    rawBody
            );
        } catch (RuntimeException e) {
            log.warn("wechat refund notify parse / verify failed", e);
            return GatewayRefundNotifyResult.invalid(rawBody);
        }
    }

    private long toLongCents(BigDecimal amountYuan) {
        return amountYuan.movePointRight(2).longValueExact();
    }

    private RefundStatus mapRefundStatus(@Nullable Status status) {
        if (status == null) {
            return RefundStatus.REFUNDING;
        }
        return switch (status) {
            case SUCCESS -> RefundStatus.REFUNDED;
            case CLOSED -> RefundStatus.FAILED;
            case PROCESSING -> RefundStatus.REFUNDING;
            case ABNORMAL -> RefundStatus.REFUNDING;   // 异常需人工介入,先 RFUNDING 占位
        };
    }

    private RefundStatus mapRefundNotificationStatus(
            com.wechat.pay.java.service.refund.model.@Nullable Status status) {
        return mapRefundStatus(status);
    }

    private String buildRefundNotifyUrl() {
        if (notifyBaseUrl == null || notifyBaseUrl.isEmpty()) {
            return "";
        }
        return notifyBaseUrl + "/payment/wechat/refund-notify";
    }

    @Override
    public GatewayTransferResult transfer(GatewayTransferCommand command) {
        ensureReady();
        // 微信商家转账走 transferbatch API,单笔提现作为一个 batch 包一个 detail
        InitiateBatchTransferRequest req = new InitiateBatchTransferRequest();
        req.setAppid(appId);
        req.setOutBatchNo(command.transferNo());
        req.setBatchName(command.reason() == null ? "提现" : command.reason());
        req.setBatchRemark(command.reason() == null ? "merchant transfer" : command.reason());
        req.setTotalAmount(toLongCents(command.amount()));
        req.setTotalNum(1);
        TransferDetailInput detail = new TransferDetailInput();
        detail.setOutDetailNo(command.transferNo() + "-1");
        detail.setTransferAmount(toLongCents(command.amount()));
        detail.setTransferRemark(command.reason() == null ? "提现" : command.reason());
        detail.setOpenid(command.recipientAccount());
        if (command.recipientName() != null) {
            detail.setUserName(command.recipientName());
        }
        req.setTransferDetailList(java.util.List.of(detail));
        try {
            InitiateBatchTransferResponse resp = transferBatchService.initiateBatchTransfer(req);
            TransferStatus status = mapBatchStatus(resp.getBatchStatus() == null
                    ? null : resp.getBatchStatus().toString());
            return new GatewayTransferResult(resp.getBatchId(), status,
                    status == TransferStatus.SUCCESS ? LocalDateTime.now() : null, null);
        } catch (RuntimeException e) {
            throw TransferErrorCode.TRANSFER_GATEWAY_ERROR.toServiceException(e);
        }
    }

    @Override
    public GatewayTransferResult queryTransfer(PaymentChannel channel, String transferNo) {
        ensureReady();
        try {
            GetTransferBatchByOutNoRequest req = new GetTransferBatchByOutNoRequest();
            req.setOutBatchNo(transferNo);
            req.setNeedQueryDetail(false);
            TransferBatchEntity resp = transferBatchService.getTransferBatchByOutNo(req);
            TransferBatchGet batch = resp.getTransferBatch();
            String batchStatus = batch == null || batch.getBatchStatus() == null
                    ? null : batch.getBatchStatus().toString();
            TransferStatus status = mapBatchStatus(batchStatus);
            return new GatewayTransferResult(
                    batch == null ? null : batch.getBatchId(),
                    status,
                    status == TransferStatus.SUCCESS ? LocalDateTime.now() : null,
                    status == TransferStatus.FAILED ? batchStatus : null);
        } catch (RuntimeException e) {
            throw TransferErrorCode.TRANSFER_GATEWAY_ERROR.toServiceException(e);
        }
    }

    private TransferStatus mapBatchStatus(@Nullable String batchStatus) {
        if (batchStatus == null) {
            return TransferStatus.REVIEWING;
        }
        return switch (batchStatus) {
            case "FINISHED" -> TransferStatus.SUCCESS;
            case "CLOSED" -> TransferStatus.FAILED;
            case "ACCEPTED", "PROCESSING" -> TransferStatus.REVIEWING;
            default -> TransferStatus.REVIEWING;
        };
    }

    private void ensureReady() {
        if (config == null) {
            throw PaymentErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
    }
}
