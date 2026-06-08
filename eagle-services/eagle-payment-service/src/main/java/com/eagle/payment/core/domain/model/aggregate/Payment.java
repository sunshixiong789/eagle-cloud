package com.eagle.payment.core.domain.model.aggregate;

import com.eagle.datajpa.base.BaseAggregateRoot;
import com.eagle.payment.core.common.exception.PaymentErrorCode;
import com.eagle.payment.core.domain.event.PaymentCancelledEvent;
import com.eagle.payment.core.domain.event.PaymentExpiredEvent;
import com.eagle.payment.core.domain.event.PaymentFailedEvent;
import com.eagle.payment.core.domain.event.PaymentPaidEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
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
 * 支付订单聚合根。
 *
 * <p>状态机详见 {@link PaymentStatus};所有状态迁移必须通过聚合根的业务方法触发,
 * 禁止外部直接 setter。
 *
 * <p>金额一律 {@link BigDecimal} (scale=2,单位元),DB 存 {@code DECIMAL(18,2)}。
 *
 * <p>幂等键: {@code (tenant_id, biz_order_no, channel)} 唯一约束;同一业务订单 + 同一
 * 渠道不允许重复下单。{@code (channel, out_trade_no)} 为渠道返回的交易号,用于回调匹配。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "t_payment", comment = "支付订单",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_biz",
                        columnNames = {"tenant_id", "biz_order_no", "channel"})
        },
        indexes = {
                @Index(name = "idx_payment_tenant_status_created",
                        columnList = "tenant_id, status, create_time"),
                @Index(name = "idx_payment_out_trade_no", columnList = "channel, out_trade_no"),
                @Index(name = "idx_payment_user", columnList = "tenant_id, user_id")
        })
public class Payment extends BaseAggregateRoot<Payment> {

    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64,
            comment = "租户 ID")
    private String tenantId;

    @Column(name = "biz_order_no", nullable = false, updatable = false, length = 64,
            comment = "业务订单号 (上游调用方提供)")
    private String bizOrderNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 16,
            comment = "支付渠道")
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "scene", nullable = false, updatable = false, length = 16,
            comment = "支付场景")
    private PaymentScene scene;

    @Column(name = "amount", nullable = false, updatable = false,
            precision = 18, scale = 2, comment = "支付金额 (元)")
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 8,
            comment = "币种")
    private String currency;

    @Column(name = "subject", nullable = false, updatable = false, length = 256,
            comment = "订单标题")
    private String subject;

    @Column(name = "user_id", updatable = false, length = 64, comment = "下单用户 ID")
    private String userId;

    @Column(name = "out_trade_no", length = 64, comment = "渠道返回的交易号")
    private String outTradeNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16, comment = "支付状态")
    private PaymentStatus status;

    @Column(name = "paid_at", comment = "支付完成时间")
    private LocalDateTime paidAt;

    @Column(name = "expires_at", nullable = false, comment = "过期时间")
    private LocalDateTime expiresAt;

    @Column(name = "refunded_amount", nullable = false, precision = 18, scale = 2,
            comment = "累计已退款金额 (元)")
    private BigDecimal refundedAmount;

    @Column(name = "fail_reason", length = 512, comment = "失败原因 (failed 状态)")
    private String failReason;

    private Payment(String tenantId, String bizOrderNo, PaymentChannel channel, PaymentScene scene,
                    BigDecimal amount, String currency, String subject, @Nullable String userId,
                    LocalDateTime expiresAt) {
        this.tenantId = tenantId;
        this.bizOrderNo = bizOrderNo;
        this.channel = channel;
        this.scene = scene;
        this.amount = amount;
        this.currency = currency;
        this.subject = subject;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.status = PaymentStatus.CREATED;
        this.refundedAmount = BigDecimal.ZERO;
    }

    /**
     * 创建一个 CREATED 状态的支付订单。聚合根不知道渠道结果,outTradeNo 由 {@link #submittedToChannel}
     * 在收到渠道返回后回填。
     */
    public static Payment create(String tenantId, String bizOrderNo, PaymentChannel channel,
                                 PaymentScene scene, BigDecimal amount, String currency,
                                 String subject, @Nullable String userId,
                                 LocalDateTime expiresAt) {
        if (amount == null || amount.signum() <= 0) {
            throw PaymentErrorCode.INVALID_AMOUNT.toDomainException();
        }
        return new Payment(tenantId, bizOrderNo, channel, scene, amount.setScale(2),
                currency, subject, userId, expiresAt);
    }

    /**
     * 已成功提交到渠道,迁移到 PAYING 并回填渠道交易号。
     */
    public void submittedToChannel(String outTradeNo) {
        if (this.status != PaymentStatus.CREATED) {
            throw PaymentErrorCode.INVALID_STATUS.toDomainException();
        }
        this.status = PaymentStatus.PAYING;
        this.outTradeNo = outTradeNo;
    }

    /**
     * 渠道回调通知支付成功。允许从 CREATED / PAYING 迁移到 PAID(渠道极少情况下未走 submittedToChannel
     * 直接成功)。重复回调由调用方通过 status 判断幂等,本方法对终态调用抛 INVALID_STATUS。
     */
    public void markPaid(LocalDateTime paidAt, @Nullable String outTradeNo) {
        if (this.status == PaymentStatus.PAID) {
            return;
        }
        if (this.status != PaymentStatus.CREATED && this.status != PaymentStatus.PAYING) {
            throw PaymentErrorCode.INVALID_STATUS.toDomainException();
        }
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
        if (outTradeNo != null) {
            this.outTradeNo = outTradeNo;
        }
        registerEvent(new PaymentPaidEvent(getId(), tenantId, bizOrderNo, channel,
                amount, currency, this.outTradeNo, paidAt));
    }

    /**
     * 渠道回调通知支付失败。
     */
    public void markFailed(String reason) {
        if (this.status == PaymentStatus.FAILED) {
            return;
        }
        if (this.status != PaymentStatus.CREATED && this.status != PaymentStatus.PAYING) {
            throw PaymentErrorCode.INVALID_STATUS.toDomainException();
        }
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
        registerEvent(new PaymentFailedEvent(getId(), tenantId, bizOrderNo, channel, reason));
    }

    /**
     * 主动取消,仅允许 CREATED / PAYING 迁移到 CANCELLED。
     */
    public void cancel(String reason) {
        if (this.status != PaymentStatus.CREATED && this.status != PaymentStatus.PAYING) {
            throw PaymentErrorCode.INVALID_STATUS.toDomainException();
        }
        this.status = PaymentStatus.CANCELLED;
        this.failReason = reason;
        registerEvent(new PaymentCancelledEvent(getId(), tenantId, bizOrderNo, channel, reason));
    }

    /**
     * 标记为过期(由定时任务调用)。
     */
    public void markExpired() {
        if (this.status != PaymentStatus.CREATED && this.status != PaymentStatus.PAYING) {
            throw PaymentErrorCode.INVALID_STATUS.toDomainException();
        }
        this.status = PaymentStatus.EXPIRED;
        registerEvent(new PaymentExpiredEvent(getId(), tenantId, bizOrderNo, channel));
    }

    /**
     * 累加退款金额并校验。由 Refund 聚合根在退款成功时调用 (P0-2 引入)。
     */
    public void accumulateRefund(BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw PaymentErrorCode.INVALID_AMOUNT.toDomainException();
        }
        if (this.status != PaymentStatus.PAID) {
            throw PaymentErrorCode.INVALID_STATUS.toDomainException();
        }
        BigDecimal next = this.refundedAmount.add(refundAmount);
        if (next.compareTo(this.amount) > 0) {
            // 这里复用 INVALID_AMOUNT 表达"退款超过可退余额";细分错误码由 Refund 域承担
            throw PaymentErrorCode.INVALID_AMOUNT.toDomainException();
        }
        this.refundedAmount = next;
    }

    /** 可退款余额 = amount - refundedAmount。 */
    public BigDecimal refundableAmount() {
        return this.amount.subtract(this.refundedAmount);
    }

    public boolean isExpired(LocalDateTime now) {
        return now != null && expiresAt != null && now.isAfter(expiresAt);
    }
}
