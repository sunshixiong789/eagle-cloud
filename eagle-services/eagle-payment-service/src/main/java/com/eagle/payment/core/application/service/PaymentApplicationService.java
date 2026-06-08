package com.eagle.payment.core.application.service;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.payment.core.common.exception.PaymentErrorCode;
import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.port.GatewayPayCommand;
import com.eagle.payment.core.domain.port.GatewayPayResult;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import com.eagle.payment.core.interfaces.dto.request.CreatePaymentRequest;
import com.eagle.payment.core.interfaces.dto.response.CreatePaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Payment 应用服务:用例编排 / 事务边界。
 *
 * <p>下单核心流程:
 * <ol>
 *   <li>幂等检查 - (bizOrderNo, channel) UNIQUE,DB 兜底</li>
 *   <li>创建 CREATED 状态聚合根,生成 outTradeNo (Snowflake)</li>
 *   <li>提交到渠道,渠道返回 channelTradeNo + payload</li>
 *   <li>聚合根 submittedToChannel,迁移到 PAYING</li>
 *   <li>save 触发 IDENTITY 落库,事件由 Spring Data {@code AbstractAggregateRoot} 自动发布</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final IdGenerator idGenerator;
    private final PaymentProperties properties;
    private final Map<PaymentChannel, PaymentGatewayPort> gateways;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                     IdGenerator idGenerator,
                                     PaymentProperties properties,
                                     List<PaymentGatewayPort> gatewayPorts) {
        this.paymentRepository = paymentRepository;
        this.idGenerator = idGenerator;
        this.properties = properties;
        Map<PaymentChannel, PaymentGatewayPort> map = new EnumMap<>(PaymentChannel.class);
        for (PaymentGatewayPort port : gatewayPorts) {
            map.put(port.getChannel(), port);
        }
        this.gateways = map;
    }

    /**
     * 创建支付订单并提交到渠道。
     */
    @Transactional
    public CreatePaymentResponse create(CreatePaymentRequest request) {
        PaymentChannel channel = request.getChannel();
        if (paymentRepository.existsByBizOrderNoAndChannel(request.getBizOrderNo(), channel)) {
            throw PaymentErrorCode.DUPLICATE_PAYMENT.toConflictException();
        }
        PaymentGatewayPort gateway = gateways.get(channel);
        if (gateway == null) {
            throw PaymentErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
        int expireMinutes = request.getExpireMinutes() != null
                ? request.getExpireMinutes() : properties.getExpireMinutes();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expireMinutes);
        Payment payment = Payment.create(
                request.getBizOrderNo(),
                channel,
                request.getScene(),
                request.getAmount(),
                request.getCurrency() == null ? "CNY" : request.getCurrency(),
                request.getSubject(),
                request.getUserId(),
                expiresAt
        );
        try {
            payment = paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            // Mode A 兜底:并发下两个相同 bizOrderNo 同时进 existsBy 检查通过,落库时唯一约束抛
            if (paymentRepository.existsByBizOrderNoAndChannel(request.getBizOrderNo(), channel)) {
                throw PaymentErrorCode.DUPLICATE_PAYMENT.toConflictException();
            }
            throw e;
        }

        String outTradeNo = "PAY" + idGenerator.nextIdStr();
        GatewayPayCommand command = new GatewayPayCommand(
                channel,
                request.getScene(),
                outTradeNo,
                request.getAmount(),
                request.getCurrency() == null ? "CNY" : request.getCurrency(),
                request.getSubject(),
                request.getUserId(),
                expiresAt,
                request.getClientIp(),
                request.getReturnUrl(),
                null,
                request.getOpenId()
        );
        GatewayPayResult result = gateway.createPayment(command);
        payment.submittedToChannel(outTradeNo);
        Payment saved = paymentRepository.save(payment);
        log.info("payment created, id={}, channel={}, scene={}, bizOrderNo={}, outTradeNo={}",
                saved.getId(), channel, request.getScene(), request.getBizOrderNo(), outTradeNo);
        return new CreatePaymentResponse(saved.getId(), outTradeNo,
                result.payload(), result.payloadType());
    }

    /**
     * 主动取消支付订单 (CREATED / PAYING 允许)。
     */
    @Transactional
    public void cancel(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentErrorCode.PAYMENT_NOT_FOUND::toNotFoundException);
        payment.cancel(reason == null ? "user cancelled" : reason);
        paymentRepository.save(payment);
        log.info("payment cancelled, id={}, reason={}", paymentId, reason);
    }

    /**
     * 查询支付订单详情。
     */
    @Transactional(readOnly = true)
    public Payment findById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(PaymentErrorCode.PAYMENT_NOT_FOUND::toNotFoundException);
    }

    /**
     * 按业务订单号 + 渠道查询。
     */
    @Transactional(readOnly = true)
    public Payment findByBizOrderNo(String bizOrderNo, PaymentChannel channel) {
        return paymentRepository.findByBizOrderNoAndChannel(bizOrderNo, channel)
                .orElseThrow(PaymentErrorCode.PAYMENT_NOT_FOUND::toNotFoundException);
    }
}
