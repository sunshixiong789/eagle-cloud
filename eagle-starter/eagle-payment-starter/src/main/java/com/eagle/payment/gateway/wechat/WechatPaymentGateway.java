package com.eagle.payment.gateway.wechat;

import com.eagle.payment.gateway.PaymentGateway;
import com.eagle.payment.model.NotifyResult;
import com.eagle.payment.model.PayRequest;
import com.eagle.payment.model.PayResult;
import com.eagle.payment.model.RefundRequest;
import com.eagle.payment.model.RefundResult;
import com.eagle.payment.properties.PaymentProperties;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 微信支付网关实现。
 *
 * <p>基于 wechatpay-java SDK 实现统一支付接口，以小程序/公众号 JSAPI 支付为主。
 * 使用 {@link RSAAutoCertificateConfig} 自动管理平台证书，无需手动下载和更新。
 *
 * <p>支付金额单位转换：微信支付使用分（整数），本接口使用元（BigDecimal）。
 *
 * @author eagle
 */
@Slf4j
public class WechatPaymentGateway implements PaymentGateway {

    /**
     * 元转分系数
     */
    private static final int YUAN_TO_FEN = 100;

    private final PaymentProperties.Wechat wechatProps;
    private RSAAutoCertificateConfig config;
    private JsapiServiceExtension jsapiService;
    private RefundService refundService;
    private NotificationParser notificationParser;

    /**
     * 构造微信支付网关。
     *
     * @param wechatProps 微信支付配置属性
     */
    public WechatPaymentGateway(PaymentProperties.Wechat wechatProps) {
        this.wechatProps = wechatProps;
    }

    /**
     * 初始化微信支付 SDK 客户端，在 Bean 注入完成后执行。
     *
     * <p>{@link RSAAutoCertificateConfig} 会在后台定期自动刷新平台证书，
     * 无需业务代码介入。
     */
    @PostConstruct
    public void init() {
        this.config = new RSAAutoCertificateConfig.Builder()
                .merchantId(wechatProps.getMchId())
                .privateKey(wechatProps.getPrivateKey())
                .merchantSerialNumber(wechatProps.getMchSerialNo())
                .apiV3Key(wechatProps.getApiV3Key())
                .build();

        this.jsapiService = new JsapiServiceExtension.Builder()
                .config(config)
                .build();

        this.refundService = new RefundService.Builder()
                .config(config)
                .build();

        this.notificationParser = new NotificationParser((NotificationConfig) config);
        log.info("WechatPaymentGateway initialized, mchId: {}", wechatProps.getMchId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>调用 JSAPI 支付预下单接口，返回的 {@link PayResult#getPayInfo()} 为
     * 前端调起 {@code wx.requestPayment} 所需的 JSON 字符串（含签名）。
     * {@link PayRequest#getOpenId()} 为必填字段。
     */
    @Override
    public PayResult pay(PayRequest request) {
        try {
            PrepayRequest prepayRequest = new PrepayRequest();
            prepayRequest.setAppid(wechatProps.getAppId());
            prepayRequest.setMchid(wechatProps.getMchId());
            prepayRequest.setDescription(request.getSubject());
            prepayRequest.setOutTradeNo(request.getOutTradeNo());
            prepayRequest.setNotifyUrl(wechatProps.getNotifyUrl());

            // 设置金额：微信支付单位为分
            com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
                    new com.wechat.pay.java.service.payments.jsapi.model.Amount();
            amount.setTotal(yuanToFen(request.getAmount()));
            prepayRequest.setAmount(amount);

            // 设置付款方
            com.wechat.pay.java.service.payments.jsapi.model.Payer payer =
                    new com.wechat.pay.java.service.payments.jsapi.model.Payer();
            payer.setOpenid(request.getOpenId());
            prepayRequest.setPayer(payer);

            PrepayWithRequestPaymentResponse response =
                    jsapiService.prepayWithRequestPayment(prepayRequest);

            // 将前端所需的支付参数序列化为 JSON 字符串返回
            String payInfo = buildPayInfo(response);
            // getPackageVal() returns "prepay_id=xxx"; extract the id portion
            String packageVal = response.getPackageVal();
            String prepayId = (packageVal != null && packageVal.startsWith("prepay_id="))
                    ? packageVal.substring("prepay_id=".length()) : packageVal;

            return PayResult.builder()
                    .success(true)
                    .outTradeNo(request.getOutTradeNo())
                    .tradeNo(prepayId)
                    .payInfo(payInfo)
                    .build();
        } catch (ServiceException e) {
            log.warn("Wechat pay failed, outTradeNo: {}, errorCode: {}, errorMessage: {}",
                    request.getOutTradeNo(), e.getErrorCode(), e.getErrorMessage());
            return PayResult.builder()
                    .success(false)
                    .outTradeNo(request.getOutTradeNo())
                    .errorMessage(e.getErrorMessage())
                    .build();
        } catch (Exception e) {
            log.error("Wechat pay exception, outTradeNo: {}", request.getOutTradeNo(), e);
            return PayResult.builder()
                    .success(false)
                    .outTradeNo(request.getOutTradeNo())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>调用微信退款接口，退款金额单位自动从元转换为分。
     */
    @Override
    public RefundResult refund(RefundRequest request) {
        try {
            CreateRequest createRequest = new CreateRequest();
            createRequest.setOutTradeNo(request.getOutTradeNo());
            createRequest.setOutRefundNo(request.getRefundNo());
            createRequest.setReason(request.getReason());

            AmountReq amountReq = new AmountReq();
            amountReq.setRefund(yuanToFen(request.getRefundAmount()).longValue());
            // 总金额通过查询订单获取；此处用退款金额代替，实际业务应传入原订单金额
            amountReq.setTotal(yuanToFen(request.getRefundAmount()).longValue());
            amountReq.setCurrency("CNY");
            createRequest.setAmount(amountReq);

            Refund refund = refundService.create(createRequest);
            return RefundResult.builder()
                    .success(true)
                    .refundNo(refund.getOutRefundNo())
                    .outTradeNo(refund.getOutTradeNo())
                    .build();
        } catch (ServiceException e) {
            log.warn("Wechat refund failed, outTradeNo: {}, errorCode: {}, errorMessage: {}",
                    request.getOutTradeNo(), e.getErrorCode(), e.getErrorMessage());
            return RefundResult.builder()
                    .success(false)
                    .refundNo(request.getRefundNo())
                    .outTradeNo(request.getOutTradeNo())
                    .errorMessage(e.getErrorMessage())
                    .build();
        } catch (Exception e) {
            log.error("Wechat refund exception, outTradeNo: {}", request.getOutTradeNo(), e);
            return RefundResult.builder()
                    .success(false)
                    .refundNo(request.getRefundNo())
                    .outTradeNo(request.getOutTradeNo())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>使用 {@link NotificationParser} 验签并解析微信支付回调。
     * 签名验证由 SDK 自动完成（基于平台证书），无需手动验签。
     * {@code params} 中需包含微信回调头部信息：{@code Wechatpay-Timestamp}、
     * {@code Wechatpay-Nonce}、{@code Wechatpay-Signature}、{@code Wechatpay-Serial}。
     */
    @Override
    public NotifyResult parseNotify(Map<String, String> params, String body) {
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(params.getOrDefault("Wechatpay-Serial", ""))
                    .nonce(params.getOrDefault("Wechatpay-Nonce", ""))
                    .signature(params.getOrDefault("Wechatpay-Signature", ""))
                    .timestamp(params.getOrDefault("Wechatpay-Timestamp", ""))
                    .body(body)
                    .build();

            Transaction transaction = notificationParser.parse(requestParam, Transaction.class);
            boolean success = Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState());

            BigDecimal amount = BigDecimal.ZERO;
            if (transaction.getAmount() != null && transaction.getAmount().getTotal() != null) {
                amount = fenToYuan(transaction.getAmount().getTotal());
            }

            return NotifyResult.builder()
                    .success(success)
                    .outTradeNo(transaction.getOutTradeNo())
                    .tradeNo(transaction.getTransactionId())
                    .amount(amount)
                    .buyerId(transaction.getPayer() != null ? transaction.getPayer().getOpenid() : null)
                    .build();
        } catch (Exception e) {
            log.error("Wechat notify parse exception", e);
            return NotifyResult.builder().success(false).build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>通过商户订单号调用微信查单接口。
     */
    @Override
    public PayResult queryOrder(String outTradeNo) {
        try {
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(wechatProps.getMchId());
            queryRequest.setOutTradeNo(outTradeNo);

            Transaction transaction = jsapiService.queryOrderByOutTradeNo(queryRequest);
            boolean success = Transaction.TradeStateEnum.SUCCESS.equals(transaction.getTradeState());

            return PayResult.builder()
                    .success(success)
                    .outTradeNo(outTradeNo)
                    .tradeNo(transaction.getTransactionId())
                    .payInfo(transaction.getTradeState() != null
                            ? transaction.getTradeState().name() : null)
                    .build();
        } catch (ServiceException e) {
            log.warn("Wechat queryOrder failed, outTradeNo: {}, errorCode: {}",
                    outTradeNo, e.getErrorCode());
            return PayResult.builder()
                    .success(false)
                    .outTradeNo(outTradeNo)
                    .errorMessage(e.getErrorMessage())
                    .build();
        } catch (Exception e) {
            log.error("Wechat queryOrder exception, outTradeNo: {}", outTradeNo, e);
            return PayResult.builder()
                    .success(false)
                    .outTradeNo(outTradeNo)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 将 {@link PrepayWithRequestPaymentResponse} 序列化为前端所需的 JSON 字符串。
     *
     * <p>包含 appId、timeStamp、nonceStr、package（prepay_id）、signType、paySign。
     */
    private String buildPayInfo(PrepayWithRequestPaymentResponse response) {
        return String.format(
                "{\"appId\":\"%s\",\"timeStamp\":\"%s\",\"nonceStr\":\"%s\","
                        + "\"package\":\"%s\",\"signType\":\"%s\",\"paySign\":\"%s\"}",
                response.getAppId(),
                response.getTimeStamp(),
                response.getNonceStr(),
                response.getPackageVal(),
                response.getSignType(),
                response.getPaySign()
        );
    }

    /**
     * 元转分（微信支付金额单位为分）。
     */
    private Integer yuanToFen(BigDecimal yuan) {
        return yuan.multiply(BigDecimal.valueOf(YUAN_TO_FEN))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 分转元。
     */
    private BigDecimal fenToYuan(long fen) {
        return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(YUAN_TO_FEN), 2, RoundingMode.HALF_UP);
    }
}
