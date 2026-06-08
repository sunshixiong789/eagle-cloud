package com.eagle.payment.core.domain.repository;

import com.eagle.payment.core.domain.model.aggregate.Refund;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Refund 聚合根 Repository。
 *
 * @author sunshixiong
 */
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /** 业务退款号 + 租户唯一查询 (幂等键)。 */
    Optional<Refund> findByTenantIdAndBizRefundNo(String tenantId, String bizRefundNo);

    /** 渠道退款号匹配 (异步回调)。 */
    Optional<Refund> findByChannelAndChannelRefundNo(PaymentChannel channel, String channelRefundNo);

    /** 是否已存在 (DataIntegrityViolation 兜底)。 */
    boolean existsByTenantIdAndBizRefundNo(String tenantId, String bizRefundNo);

    /** 列出指定 Payment 下的所有 Refund。 */
    List<Refund> findByPaymentId(Long paymentId);

    /**
     * 计算指定 Payment 的累计 REFUNDED 金额 (聚合查询)。用于退款发起时的对账校验,
     * 兜底 Payment.refundedAmount 与 Refund 实际终态不一致的场景。
     */
    default BigDecimal sumRefundedAmountByPaymentId(Long paymentId) {
        return findByPaymentId(paymentId).stream()
                .filter(r -> r.getStatus() == com.eagle.payment.core.domain.model.enums.RefundStatus.REFUNDED)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
