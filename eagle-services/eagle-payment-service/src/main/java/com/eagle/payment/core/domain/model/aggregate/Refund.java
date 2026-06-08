package com.eagle.payment.core.domain.model.aggregate;

import com.eagle.datajpa.base.BaseAggregateRoot;
import com.eagle.payment.core.common.exception.RefundErrorCode;
import com.eagle.payment.core.domain.event.RefundCompletedEvent;
import com.eagle.payment.core.domain.event.RefundFailedEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款聚合根。
 *
 * <p>跨聚合引用 {@link Payment} 只存 {@code paymentId} (按 06-database.md);引用关系完整性
 * 由应用层保证 (查 Payment.findById 不存在则不允许创建 Refund)。
 *
 * <p>幂等键: {@code (biz_refund_no)} UNIQUE;同一业务退款号只能发起一次。
 * {@code (channel, channel_refund_no)} 索引用于回调匹配。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "t_refund", comment = "退款单",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refund_biz",
                        columnNames = {"biz_refund_no"})
        },
        indexes = {
                @Index(name = "idx_refund_payment", columnList = "payment_id"),
                @Index(name = "idx_refund_status_created",
                        columnList = "status, create_time"),
                @Index(name = "idx_refund_channel_no",
                        columnList = "channel, channel_refund_no")
        })
public class Refund extends BaseAggregateRoot<Refund> {

    @Column(name = "payment_id", nullable = false, updatable = false,
            comment = "关联 Payment ID (跨聚合 ID 引用)")
    private Long paymentId;

    @Column(name = "biz_refund_no", nullable = false, updatable = false, length = 64,
            comment = "业务退款号 (上游调用方提供)")
    private String bizRefundNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 16,
            comment = "支付渠道")
    private PaymentChannel channel;

    @Column(name = "amount", nullable = false, updatable = false,
            precision = 18, scale = 2, comment = "退款金额 (元)")
    private BigDecimal amount;

    @Column(name = "reason", length = 512, comment = "退款原因")
    private String reason;

    @Column(name = "channel_refund_no", length = 64, comment = "渠道返回的退款单号")
    private String channelRefundNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16, comment = "退款状态")
    private RefundStatus status;

    @Column(name = "refunded_at", comment = "退款完成时间")
    private LocalDateTime refundedAt;

    @Column(name = "fail_reason", length = 512, comment = "失败原因")
    private String failReason;

    private Refund(Long paymentId, String bizRefundNo, PaymentChannel channel,
                   BigDecimal amount, @Nullable String reason) {
        this.paymentId = paymentId;
        this.bizRefundNo = bizRefundNo;
        this.channel = channel;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
    }

    /**
     * 创建 PENDING 状态的退款单。<strong>调用方</strong>负责:
     * <ul>
     *   <li>验证 Payment.status == PAID</li>
     *   <li>验证 amount &lt;= Payment.refundableAmount()</li>
     *   <li>(可选)部分退款开关校验</li>
     * </ul>
     * Refund 聚合根本身只校验 amount &gt; 0 与状态机一致性,跨聚合校验由应用服务承担。
     */
    public static Refund create(Long paymentId, String bizRefundNo,
                                PaymentChannel channel, BigDecimal amount,
                                @Nullable String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw RefundErrorCode.INVALID_REFUND_AMOUNT.toDomainException();
        }
        return new Refund(paymentId, bizRefundNo, channel, amount.setScale(2), reason);
    }

    /**
     * 已提交到渠道,迁移到 REFUNDING 并回填渠道退款单号。
     */
    public void submittedToChannel(String channelRefundNo) {
        if (this.status != RefundStatus.PENDING) {
            throw RefundErrorCode.INVALID_REFUND_STATUS.toDomainException();
        }
        this.status = RefundStatus.REFUNDING;
        this.channelRefundNo = channelRefundNo;
    }

    /**
     * 渠道回调通知退款成功。允许从 PENDING / REFUNDING 迁移到 REFUNDED (极少情况下渠道
     * 可能直接同步返回成功跳过 REFUNDING)。重复回调对终态短路。
     */
    public void markRefunded(LocalDateTime refundedAt, @Nullable String channelRefundNo) {
        if (this.status == RefundStatus.REFUNDED) {
            return;
        }
        if (this.status != RefundStatus.PENDING && this.status != RefundStatus.REFUNDING) {
            throw RefundErrorCode.INVALID_REFUND_STATUS.toDomainException();
        }
        this.status = RefundStatus.REFUNDED;
        this.refundedAt = refundedAt;
        if (channelRefundNo != null) {
            this.channelRefundNo = channelRefundNo;
        }
        registerEvent(new RefundCompletedEvent(getId(), paymentId, bizRefundNo,
                channel, amount, this.channelRefundNo, refundedAt));
    }

    /**
     * 渠道回调通知退款失败。
     */
    public void markFailed(String reason) {
        if (this.status == RefundStatus.FAILED) {
            return;
        }
        if (this.status != RefundStatus.PENDING && this.status != RefundStatus.REFUNDING) {
            throw RefundErrorCode.INVALID_REFUND_STATUS.toDomainException();
        }
        this.status = RefundStatus.FAILED;
        this.failReason = reason;
        registerEvent(new RefundFailedEvent(getId(), paymentId, bizRefundNo,
                channel, amount, reason));
    }
}
