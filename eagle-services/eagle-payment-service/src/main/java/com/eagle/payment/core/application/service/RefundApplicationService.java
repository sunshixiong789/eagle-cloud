package com.eagle.payment.core.application.service;

import com.eagle.payment.core.common.exception.PaymentErrorCode;
import com.eagle.payment.core.common.exception.RefundErrorCode;
import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.aggregate.Refund;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.model.enums.RefundStatus;
import com.eagle.payment.core.domain.port.GatewayRefundCommand;
import com.eagle.payment.core.domain.port.GatewayRefundResult;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import com.eagle.payment.core.domain.repository.RefundRepository;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import com.eagle.payment.core.interfaces.dto.request.CreateRefundRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Refund 应用服务: 用例编排 / 事务边界。
 *
 * <p>发起退款流程:
 * <ol>
 *   <li>查 Payment, 校验 status == PAID</li>
 *   <li>校验 amount 合法 (&gt; 0 且 &lt;= Payment.refundableAmount)</li>
 *   <li>校验部分退款开关 (eagle.payment.refund.allow-partial)</li>
 *   <li>幂等检查 - (bizRefundNo) UNIQUE,DB 兜底 (Mode A)</li>
 *   <li>创建 Refund (PENDING) → 提交到渠道 → submittedToChannel / markRefunded</li>
 *   <li>同步成功的 (支付宝)立即累加 Payment.refundedAmount;异步成功的 (微信) 由回调推进</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
public class RefundApplicationService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentProperties properties;
    private final Map<PaymentChannel, PaymentGatewayPort> gateways;

    public RefundApplicationService(RefundRepository refundRepository,
                                    PaymentRepository paymentRepository,
                                    PaymentProperties properties,
                                    List<PaymentGatewayPort> gatewayPorts) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.properties = properties;
        Map<PaymentChannel, PaymentGatewayPort> map = new EnumMap<>(PaymentChannel.class);
        for (PaymentGatewayPort port : gatewayPorts) {
            map.put(port.getChannel(), port);
        }
        this.gateways = map;
    }

    @Transactional
    public Refund create(CreateRefundRequest request) {
        if (refundRepository.existsByBizRefundNo(request.getBizRefundNo())) {
            throw RefundErrorCode.DUPLICATE_REFUND.toConflictException();
        }
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(PaymentErrorCode.PAYMENT_NOT_FOUND::toNotFoundException);
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw RefundErrorCode.PAYMENT_NOT_PAID.toDomainException();
        }
        if (request.getAmount().compareTo(payment.refundableAmount()) > 0) {
            throw RefundErrorCode.EXCEED_REFUNDABLE.toDomainException();
        }
        // 部分退款开关:amount < 原订单金额且没有累计退款记录时,等价"部分退款";
        // 关闭开关时只允许全额退款 (amount == refundableAmount 且 refundableAmount == amount)
        if (!properties.getRefund().isAllowPartial()
                && request.getAmount().compareTo(payment.getAmount()) != 0) {
            throw RefundErrorCode.PARTIAL_DISABLED.toDomainException();
        }

        Refund refund = Refund.create(payment.getId(), request.getBizRefundNo(),
                payment.getChannel(), request.getAmount(), request.getReason());
        try {
            refund = refundRepository.saveAndFlush(refund);
        } catch (DataIntegrityViolationException e) {
            if (refundRepository.existsByBizRefundNo(request.getBizRefundNo())) {
                throw RefundErrorCode.DUPLICATE_REFUND.toConflictException();
            }
            throw e;
        }

        PaymentGatewayPort gateway = gateways.get(payment.getChannel());
        if (gateway == null) {
            throw PaymentErrorCode.CHANNEL_UNAVAILABLE.toDomainException();
        }
        GatewayRefundResult result = gateway.refund(new GatewayRefundCommand(
                payment.getChannel(),
                payment.getOutTradeNo(),
                refund.getBizRefundNo(),
                refund.getAmount(),
                payment.getAmount(),
                payment.getCurrency(),
                refund.getReason()
        ));
        if (result.status() == RefundStatus.REFUNDED) {
            refund.submittedToChannel(result.channelRefundNo());
            refund.markRefunded(
                    result.refundedAt() != null ? result.refundedAt() : LocalDateTime.now(),
                    result.channelRefundNo());
            payment.accumulateRefund(refund.getAmount());
            paymentRepository.save(payment);
        } else if (result.status() == RefundStatus.FAILED) {
            refund.markFailed(result.failReason());
        } else {
            refund.submittedToChannel(result.channelRefundNo());
        }
        Refund saved = refundRepository.save(refund);
        log.info("refund created, id={}, paymentId={}, status={}, channelRefundNo={}",
                saved.getId(), payment.getId(), saved.getStatus(), saved.getChannelRefundNo());
        return saved;
    }

    @Transactional(readOnly = true)
    public Refund findById(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(RefundErrorCode.REFUND_NOT_FOUND::toNotFoundException);
    }

    @Transactional(readOnly = true)
    public Refund findByBizRefundNo(String bizRefundNo) {
        return refundRepository.findByBizRefundNo(bizRefundNo)
                .orElseThrow(RefundErrorCode.REFUND_NOT_FOUND::toNotFoundException);
    }
}
