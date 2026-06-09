package com.eagle.payment.core.domain.model.aggregate;

import com.eagle.datajpa.base.BaseAggregateRoot;
import com.eagle.payment.core.common.exception.TransferErrorCode;
import com.eagle.payment.core.domain.event.TransferApprovedEvent;
import com.eagle.payment.core.domain.event.TransferFailedEvent;
import com.eagle.payment.core.domain.event.TransferRejectedEvent;
import com.eagle.payment.core.domain.event.TransferReturnedEvent;
import com.eagle.payment.core.domain.event.TransferSucceededEvent;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
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
 * 提现 / B2C 转账聚合根。
 *
 * <p>资金从商户账户出账,打到收款方账户。状态机详见 {@link TransferStatus}。
 *
 * <p>{@link #recipientAccount} 与 {@link #recipientName} 在生产环境通常需要加密
 * (eagle-encrypt-starter);v1 先以明文 + DB 列字符串形式落库,P1 后期 hook 进
 * AttributeConverter 加密。
 *
 * <p>幂等键: {@code (biz_transfer_no)} UNIQUE。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "t_transfer", comment = "提现单",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transfer_biz",
                        columnNames = {"biz_transfer_no"})
        },
        indexes = {
                @Index(name = "idx_transfer_user_id", columnList = "user_id"),
                @Index(name = "idx_transfer_status_created",
                        columnList = "status, create_time"),
                @Index(name = "idx_transfer_channel_no",
                        columnList = "channel, channel_transfer_no"),
                @Index(name = "idx_transfer_recipient",
                        columnList = "recipient_account"),
                @Index(name = "idx_transfer_mode_status",
                        columnList = "mode, status, create_time")
        })
public class Transfer extends BaseAggregateRoot<Transfer> {

    @Column(name = "user_id", nullable = false, updatable = false,
            comment = "发起提现的用户 ID")
    private Long userId;

    @Column(name = "biz_transfer_no", nullable = false, updatable = false, length = 64,
            comment = "业务提现号 (上游调用方提供)")
    private String bizTransferNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 16,
            comment = "渠道")
    private PaymentChannel channel;

    @Column(name = "recipient_account", nullable = false, updatable = false, length = 128,
            comment = "收款方账号 (支付宝登录号 / 微信 openId)")
    private String recipientAccount;

    @Column(name = "recipient_name", updatable = false, length = 128,
            comment = "收款方姓名 (实名校验)")
    private String recipientName;

    @Column(name = "amount", nullable = false, updatable = false,
            precision = 18, scale = 2, comment = "提现金额 (元)")
    private BigDecimal amount;

    @Column(name = "reason", length = 512, comment = "提现说明")
    private String reason;

    @Column(name = "channel_transfer_no", length = 64, comment = "渠道返回的转账单号")
    private String channelTransferNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16, comment = "提现状态")
    private TransferStatus status;

    @Column(name = "succeeded_at", comment = "成功到账时间")
    private LocalDateTime succeededAt;

    @Column(name = "fail_reason", length = 512, comment = "失败原因 / 退票原因")
    private String failReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, updatable = false, length = 16,
            comment = "受理模式:IMMEDIATE 立即到账 / APPROVAL 需审核")
    private TransferMode mode;

    @Column(name = "approver_id", length = 64, comment = "审核人用户 ID")
    @Nullable
    private String approverId;

    @Column(name = "approved_at", comment = "审核通过时间")
    @Nullable
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at", comment = "审核拒绝时间")
    @Nullable
    private LocalDateTime rejectedAt;

    @Column(name = "reject_reason", length = 512, comment = "审核拒绝原因")
    @Nullable
    private String rejectReason;

    private Transfer(Long userId, String bizTransferNo, TransferMode mode, PaymentChannel channel,
                     String recipientAccount, @Nullable String recipientName,
                     BigDecimal amount, @Nullable String reason) {
        this.userId = userId;
        this.bizTransferNo = bizTransferNo;
        this.mode = mode;
        this.channel = channel;
        this.recipientAccount = recipientAccount;
        this.recipientName = recipientName;
        this.amount = amount;
        this.reason = reason;
        this.status = (mode == TransferMode.APPROVAL)
                ? TransferStatus.PENDING_APPROVAL
                : TransferStatus.PENDING;
    }

    public static Transfer create(Long userId, String bizTransferNo, TransferMode mode,
                                  PaymentChannel channel,
                                  String recipientAccount, @Nullable String recipientName,
                                  BigDecimal amount, @Nullable String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw TransferErrorCode.INVALID_TRANSFER_AMOUNT.toDomainException();
        }
        return new Transfer(userId, bizTransferNo, mode, channel, recipientAccount, recipientName,
                amount.setScale(2), reason);
    }

    /**
     * 已提交到渠道,迁移到 SUBMITTED 并回填渠道转账号。
     */
    public void submittedToChannel(String channelTransferNo) {
        if (this.status != TransferStatus.PENDING) {
            throw TransferErrorCode.INVALID_TRANSFER_STATUS.toDomainException();
        }
        this.status = TransferStatus.SUBMITTED;
        this.channelTransferNo = channelTransferNo;
    }

    /**
     * 渠道回调通知到账成功。允许从 PENDING / SUBMITTED 迁移。
     */
    public void markSucceeded(LocalDateTime succeededAt, @Nullable String channelTransferNo) {
        if (this.status == TransferStatus.SUCCESS) {
            return;
        }
        if (this.status != TransferStatus.PENDING && this.status != TransferStatus.SUBMITTED) {
            throw TransferErrorCode.INVALID_TRANSFER_STATUS.toDomainException();
        }
        this.status = TransferStatus.SUCCESS;
        this.succeededAt = succeededAt;
        if (channelTransferNo != null) {
            this.channelTransferNo = channelTransferNo;
        }
        registerEvent(new TransferSucceededEvent(getId(), bizTransferNo, channel,
                amount, recipientAccount, this.channelTransferNo, succeededAt));
    }

    /**
     * 渠道回调通知失败。
     */
    public void markFailed(String reason) {
        if (this.status == TransferStatus.FAILED) {
            return;
        }
        if (this.status != TransferStatus.PENDING && this.status != TransferStatus.SUBMITTED) {
            throw TransferErrorCode.INVALID_TRANSFER_STATUS.toDomainException();
        }
        this.status = TransferStatus.FAILED;
        this.failReason = reason;
        registerEvent(new TransferFailedEvent(getId(), bizTransferNo, channel,
                amount, recipientAccount, reason));
    }

    /**
     * 收款方退票 (扣款成功但资金原路返回);仅允许 SUCCESS 状态迁移。
     */
    public void markReturned(String reason) {
        if (this.status == TransferStatus.RETURNED) {
            return;
        }
        if (this.status != TransferStatus.SUCCESS) {
            throw TransferErrorCode.INVALID_TRANSFER_STATUS.toDomainException();
        }
        this.status = TransferStatus.RETURNED;
        this.failReason = reason;
        registerEvent(new TransferReturnedEvent(getId(), bizTransferNo, channel,
                amount, recipientAccount, reason));
    }

    /**
     * 审核通过:仅允许从 APPROVAL 模式 + PENDING_APPROVAL 状态迁出,
     * 状态先迁到 PENDING (内部过渡态,事务内由 submitToGateway 继续推进)。
     */
    public void approve(String approverId) {
        if (this.mode != TransferMode.APPROVAL) {
            throw TransferErrorCode.NOT_APPROVAL_MODE.toDomainException();
        }
        if (this.status != TransferStatus.PENDING_APPROVAL) {
            throw TransferErrorCode.APPROVAL_NOT_ALLOWED_IN_STATUS.toDomainException();
        }
        this.status = TransferStatus.PENDING;
        this.approverId = approverId;
        this.approvedAt = LocalDateTime.now();
        registerEvent(new TransferApprovedEvent(getId(), bizTransferNo, channel,
                amount, recipientAccount, approverId, this.approvedAt));
    }

    /**
     * 审核拒绝:仅允许从 PENDING_APPROVAL 迁出 → REJECTED 终态。
     */
    public void reject(String approverId, String reason) {
        if (this.status != TransferStatus.PENDING_APPROVAL) {
            throw TransferErrorCode.APPROVAL_NOT_ALLOWED_IN_STATUS.toDomainException();
        }
        this.status = TransferStatus.REJECTED;
        this.approverId = approverId;
        this.rejectReason = reason;
        this.rejectedAt = LocalDateTime.now();
        registerEvent(new TransferRejectedEvent(getId(), bizTransferNo, channel,
                amount, recipientAccount, approverId, reason, this.rejectedAt));
    }
}
