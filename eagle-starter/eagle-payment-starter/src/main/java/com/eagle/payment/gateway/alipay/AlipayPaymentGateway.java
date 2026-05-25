package com.eagle.payment.gateway.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayFundTransUniTransferRequest;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayFundTransUniTransferResponse;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.eagle.payment.gateway.PaymentGateway;
import com.eagle.payment.model.NotifyResult;
import com.eagle.payment.model.PayRequest;
import com.eagle.payment.model.PayResult;
import com.eagle.payment.model.RefundRequest;
import com.eagle.payment.model.RefundResult;
import com.eagle.payment.model.TransferRequest;
import com.eagle.payment.model.TransferResult;
import com.eagle.payment.properties.PaymentProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付网关实现。
 *
 * <p>基于支付宝 Open API SDK 实现统一支付接口。支持 APP 支付（{@code alipay.trade.app.pay}），
 * 退款使用 {@code alipay.trade.refund}，订单查询使用 {@code alipay.trade.query}。
 *
 * <p>所有 SDK 调用均捕获异常并转换为失败结果返回，避免单次支付异常影响上层业务。
 *
 * @author eagle
 */
@Slf4j
public class AlipayPaymentGateway implements PaymentGateway {

    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    private static final String TRADE_FINISHED = "TRADE_FINISHED";
    private static final DateTimeFormatter EXPIRE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaymentProperties.Alipay alipayProps;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private AlipayClient alipayClient;

    /**
     * 构造支付宝支付网关。
     *
     * @param alipayProps 支付宝配置属性
     */
    public AlipayPaymentGateway(PaymentProperties.Alipay alipayProps) {
        this.alipayProps = alipayProps;
    }

    /**
     * 初始化 AlipayClient，在 Bean 注入完成后执行。
     */
    @PostConstruct
    public void init() {
        this.alipayClient = new DefaultAlipayClient(
                alipayProps.getServerUrl(),
                alipayProps.getAppId(),
                alipayProps.getPrivateKey(),
                "json",
                alipayProps.getCharset(),
                alipayProps.getAlipayPublicKey(),
                alipayProps.getSignType()
        );
        log.info("AlipayClient initialized, appId: {}", alipayProps.getAppId());
    }

    /**
     * {@inheritDoc}
     *
     * <p>使用 {@code alipay.trade.app.pay} 接口，返回客户端拉起 SDK 所需的订单字符串。
     */
    @Override
    public PayResult pay(PayRequest request) {
        try {
            AlipayTradeAppPayRequest alipayRequest = new AlipayTradeAppPayRequest();
            alipayRequest.setNotifyUrl(alipayProps.getNotifyUrl());

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", request.getOutTradeNo());
            bizContent.put("total_amount", request.getAmount().toPlainString());
            bizContent.put("subject", request.getSubject());
            bizContent.put("body", request.getDescription());
            bizContent.put("product_code", "QUICK_MSECURITY_PAY");
            // 计算过期时间
            String timeExpire = LocalDateTime.now()
                    .plusMinutes(request.getExpireMinutes())
                    .format(EXPIRE_FORMATTER);
            bizContent.put("time_expire", timeExpire);
            if (request.getPassbackParams() != null) {
                bizContent.put("passback_params", request.getPassbackParams());
            }

            alipayRequest.setBizContent(objectMapper.writeValueAsString(bizContent));

            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(alipayRequest);
            if (response.isSuccess()) {
                return PayResult.builder()
                        .success(true)
                        .outTradeNo(request.getOutTradeNo())
                        .payInfo(response.getBody())
                        .build();
            } else {
                log.warn("Alipay pay failed, outTradeNo: {}, subCode: {}, subMsg: {}",
                        request.getOutTradeNo(), response.getSubCode(), response.getSubMsg());
                return PayResult.builder()
                        .success(false)
                        .outTradeNo(request.getOutTradeNo())
                        .errorMessage(response.getSubMsg())
                        .build();
            }
        } catch (AlipayApiException | JacksonException e) {
            log.error("Alipay pay exception, outTradeNo: {}", request.getOutTradeNo(), e);
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
     * <p>使用 {@code alipay.trade.refund} 接口发起退款。
     */
    @Override
    public RefundResult refund(RefundRequest request) {
        try {
            AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", request.getOutTradeNo());
            bizContent.put("refund_amount", request.getRefundAmount().toPlainString());
            bizContent.put("out_request_no", request.getRefundNo());
            if (request.getReason() != null) {
                bizContent.put("refund_reason", request.getReason());
            }

            alipayRequest.setBizContent(objectMapper.writeValueAsString(bizContent));

            AlipayTradeRefundResponse response = alipayClient.execute(alipayRequest);
            if (response.isSuccess()) {
                return RefundResult.builder()
                        .success(true)
                        .refundNo(request.getRefundNo())
                        .outTradeNo(request.getOutTradeNo())
                        .build();
            } else {
                log.warn("Alipay refund failed, outTradeNo: {}, subCode: {}, subMsg: {}",
                        request.getOutTradeNo(), response.getSubCode(), response.getSubMsg());
                return RefundResult.builder()
                        .success(false)
                        .refundNo(request.getRefundNo())
                        .outTradeNo(request.getOutTradeNo())
                        .errorMessage(response.getSubMsg())
                        .build();
            }
        } catch (AlipayApiException | JacksonException e) {
            log.error("Alipay refund exception, outTradeNo: {}", request.getOutTradeNo(), e);
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
     * <p>使用 {@link AlipaySignature#rsaCheckV1} 验签，验签成功且交易状态为
     * {@code TRADE_SUCCESS} 或 {@code TRADE_FINISHED} 时返回 {@code success = true}。
     */
    @Override
    public NotifyResult parseNotify(Map<String, String> params, String body) {
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProps.getAlipayPublicKey(),
                    alipayProps.getCharset(),
                    alipayProps.getSignType()
            );
            if (!signVerified) {
                log.warn("Alipay notify signature verification failed");
                return NotifyResult.builder().success(false).build();
            }

            String tradeStatus = params.get("trade_status");
            boolean success = TRADE_SUCCESS.equals(tradeStatus) || TRADE_FINISHED.equals(tradeStatus);
            String totalAmountStr = params.get("total_amount");
            BigDecimal amount = totalAmountStr != null ? new BigDecimal(totalAmountStr) : BigDecimal.ZERO;

            return NotifyResult.builder()
                    .success(success)
                    .outTradeNo(params.get("out_trade_no"))
                    .tradeNo(params.get("trade_no"))
                    .amount(amount)
                    .buyerId(params.get("buyer_id"))
                    .passbackParams(params.get("passback_params"))
                    .build();
        } catch (AlipayApiException e) {
            log.error("Alipay notify parse exception", e);
            return NotifyResult.builder().success(false).build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>使用 {@code alipay.fund.trans.uni.transfer} 接口将资金转账至支付宝账户。
     * 收款方账号填写支付宝登录号（手机号或邮箱），{@code payeeName} 为必填项，
     * 用于身份校验防范盗号风险。
     *
     * <p>调用此接口前需在支付宝开放平台开通"转账到支付宝账户"权限。
     */
    @Override
    public TransferResult transfer(TransferRequest request) {
        try {
            AlipayFundTransUniTransferRequest alipayRequest = new AlipayFundTransUniTransferRequest();

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_biz_no", request.getOutBizNo());
            bizContent.put("trans_amount", request.getAmount().toPlainString());
            bizContent.put("product_code", "TRANS_ACCOUNT_NO_PWD");
            bizContent.put("biz_scene", "DIRECT_TRANSFER");
            if (request.getRemark() != null) {
                bizContent.put("remark", request.getRemark());
            }

            // 收款方信息
            Map<String, Object> payeeInfo = new HashMap<>();
            payeeInfo.put("identity", request.getPayeeAccount());
            payeeInfo.put("identity_type", "ALIPAY_LOGON_ID");
            if (request.getPayeeName() != null) {
                payeeInfo.put("name", request.getPayeeName());
            }
            bizContent.put("payee_info", payeeInfo);

            alipayRequest.setBizContent(objectMapper.writeValueAsString(bizContent));

            AlipayFundTransUniTransferResponse response = alipayClient.execute(alipayRequest);
            if (response.isSuccess()) {
                return TransferResult.builder()
                        .success(true)
                        .orderId(response.getOrderId())
                        .outBizNo(request.getOutBizNo())
                        .build();
            } else {
                log.warn("Alipay transfer failed, outBizNo: {}, subCode: {}, subMsg: {}",
                        request.getOutBizNo(), response.getSubCode(), response.getSubMsg());
                return TransferResult.builder()
                        .success(false)
                        .outBizNo(request.getOutBizNo())
                        .errorMessage(response.getSubMsg())
                        .build();
            }
        } catch (AlipayApiException | JacksonException e) {
            log.error("Alipay transfer exception, outBizNo: {}", request.getOutBizNo(), e);
            return TransferResult.builder()
                    .success(false)
                    .outBizNo(request.getOutBizNo())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>使用 {@code alipay.trade.query} 接口主动查询订单状态。
     */
    @Override
    public PayResult queryOrder(String outTradeNo) {
        try {
            AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();

            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("out_trade_no", outTradeNo);
            alipayRequest.setBizContent(objectMapper.writeValueAsString(bizContent));

            AlipayTradeQueryResponse response = alipayClient.execute(alipayRequest);
            if (response.isSuccess()) {
                String tradeStatus = response.getTradeStatus();
                boolean success = TRADE_SUCCESS.equals(tradeStatus) || TRADE_FINISHED.equals(tradeStatus);
                return PayResult.builder()
                        .success(success)
                        .outTradeNo(outTradeNo)
                        .tradeNo(response.getTradeNo())
                        .payInfo(tradeStatus)
                        .build();
            } else {
                log.warn("Alipay queryOrder failed, outTradeNo: {}, subCode: {}",
                        outTradeNo, response.getSubCode());
                return PayResult.builder()
                        .success(false)
                        .outTradeNo(outTradeNo)
                        .errorMessage(response.getSubMsg())
                        .build();
            }
        } catch (AlipayApiException | JacksonException e) {
            log.error("Alipay queryOrder exception, outTradeNo: {}", outTradeNo, e);
            return PayResult.builder()
                    .success(false)
                    .outTradeNo(outTradeNo)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
