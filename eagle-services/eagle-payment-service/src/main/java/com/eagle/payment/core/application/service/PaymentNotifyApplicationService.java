package com.eagle.payment.core.application.service;

import com.eagle.payment.core.common.exception.PaymentErrorCode;
import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.port.GatewayNotifyResult;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回调推进 Payment 状态机。
 *
 * <p>验签由 {@link PaymentGatewayPort#parseNotify} 完成,本服务在签名 OK 后:
 * <ol>
 *   <li>按 (channel, outTradeNo) 查 Payment 聚合根;找不到记录 + warning + 返回 unknown
 *       (避免被任意外部调用方触发);</li>
 *   <li>状态机推进 - PAID 调用 {@code markPaid};FAILED 调用 {@code markFailed};</li>
 *   <li>金额校验 - 渠道返回金额必须 == 本地记录金额,不一致直接抛 SIGNATURE_INVALID
 *       (异常但具语义);</li>
 *   <li>幂等 - 二次回调到达时,markPaid / markFailed 内部已对终态短路。</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotifyApplicationService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public NotifyOutcome handle(PaymentChannel channel, GatewayNotifyResult result) {
        if (!result.signatureValid()) {
            log.warn("payment notify signature invalid, channel={}", channel);
            return NotifyOutcome.SIGNATURE_INVALID;
        }
        if (result.outTradeNo() == null) {
            log.warn("payment notify missing outTradeNo, channel={}, body={}",
                    channel, result.rawBody());
            return NotifyOutcome.UNKNOWN_PAYMENT;
        }
        Payment payment = paymentRepository
                .findByChannelAndOutTradeNo(channel, result.outTradeNo())
                .orElse(null);
        if (payment == null) {
            log.warn("payment notify references unknown outTradeNo, channel={}, outTradeNo={}",
                    channel, result.outTradeNo());
            return NotifyOutcome.UNKNOWN_PAYMENT;
        }
        if (result.amount() != null
                && payment.getAmount().compareTo(result.amount()) != 0) {
            log.warn("payment notify amount mismatch, paymentId={}, expected={}, actual={}",
                    payment.getId(), payment.getAmount(), result.amount());
            return NotifyOutcome.AMOUNT_MISMATCH;
        }
        PaymentStatus status = result.status() == null ? PaymentStatus.PAYING : result.status();
        if (status == PaymentStatus.PAID) {
            LocalDateTime paidAt = result.paidAt() != null ? result.paidAt() : LocalDateTime.now();
            payment.markPaid(paidAt, result.channelTradeNo());
        } else if (status == PaymentStatus.FAILED) {
            payment.markFailed(result.failReason());
        } else {
            log.info("payment notify with non-terminal status, paymentId={}, status={}",
                    payment.getId(), status);
            return NotifyOutcome.NON_TERMINAL;
        }
        paymentRepository.save(payment);
        return NotifyOutcome.PROCESSED;
    }

    /** 强一致金额比较 (precision safe)。 */
    @SuppressWarnings("unused")
    private static boolean equalsScale2(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) == 0;
    }

    /** 回调处理结果。 */
    public enum NotifyOutcome {
        PROCESSED,
        SIGNATURE_INVALID,
        UNKNOWN_PAYMENT,
        AMOUNT_MISMATCH,
        NON_TERMINAL;

        public boolean isAck() {
            // 渠道侧"已确认"应只在 PROCESSED 或 NON_TERMINAL (但请求合法) 时返回
            return this == PROCESSED || this == NON_TERMINAL;
        }
    }

    /** 调用方在 Controller 里映射成不同的 HTTP 响应。 */
    @SuppressWarnings("unused")
    public PaymentErrorCode mapErrorCode(NotifyOutcome outcome) {
        return switch (outcome) {
            case SIGNATURE_INVALID -> PaymentErrorCode.SIGNATURE_INVALID;
            case UNKNOWN_PAYMENT -> PaymentErrorCode.NOTIFY_UNKNOWN_PAYMENT;
            case AMOUNT_MISMATCH, NON_TERMINAL -> PaymentErrorCode.GATEWAY_ERROR;
            case PROCESSED -> null;
        };
    }
}
