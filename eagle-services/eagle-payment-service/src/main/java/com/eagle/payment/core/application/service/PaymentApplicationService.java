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
     * 创建支付订单并提交到渠道。{@code currentUserId} 来自 JWT,不允许调用方从入参指定,
     * 以杜绝"给他人创建订单"的越权场景。
     */
    @Transactional
    public CreatePaymentResponse create(CreatePaymentRequest request, Long currentUserId) {
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
                currentUserId,
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
                expiresAt,
                request.getClientIp(),
                request.getReturnUrl(),
                null,
                request.getOpenId()
        );
        GatewayPayResult result = gateway.createPayment(command);
        payment.submittedToChannel(outTradeNo);
        Payment saved = paymentRepository.save(payment);
        log.info("payment created, id={}, userId={}, channel={}, scene={}, bizOrderNo={}, outTradeNo={}",
                saved.getId(), currentUserId, channel, request.getScene(),
                request.getBizOrderNo(), outTradeNo);
        return new CreatePaymentResponse(saved.getId(), outTradeNo,
                result.payload(), result.payloadType());
    }

    /**
     * 主动取消支付订单 (CREATED / PAYING 允许)。仅允许本人取消;不属于本人的订单
     * 一律按 NOT_FOUND 返回,避免通过响应码差异泄漏订单存在性。
     */
    @Transactional
    public void cancel(Long paymentId, String reason, Long currentUserId) {
        Payment payment = loadOwned(paymentId, currentUserId);
        payment.cancel(reason == null ? "user cancelled" : reason);
        paymentRepository.save(payment);
        log.info("payment cancelled, id={}, userId={}, reason={}", paymentId, currentUserId, reason);
    }

    /**
     * 查询支付订单详情 (用户视角:仅返回归属当前用户的订单)。
     */
    @Transactional(readOnly = true)
    public Payment findById(Long paymentId, Long currentUserId) {
        return loadOwned(paymentId, currentUserId);
    }

    /**
     * 按业务订单号 + 渠道查询 (用户视角)。
     */
    @Transactional(readOnly = true)
    public Payment findByBizOrderNo(String bizOrderNo, PaymentChannel channel, Long currentUserId) {
        Payment payment = paymentRepository.findByBizOrderNoAndChannel(bizOrderNo, channel)
                .orElseThrow(PaymentErrorCode.PAYMENT_NOT_FOUND::toNotFoundException);
        if (!payment.getUserId().equals(currentUserId)) {
            throw PaymentErrorCode.PAYMENT_NOT_FOUND.toNotFoundException();
        }
        return payment;
    }

    private Payment loadOwned(Long paymentId, Long currentUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(PaymentErrorCode.PAYMENT_NOT_FOUND::toNotFoundException);
        if (!payment.getUserId().equals(currentUserId)) {
            throw PaymentErrorCode.PAYMENT_NOT_FOUND.toNotFoundException();
        }
        return payment;
    }
}
