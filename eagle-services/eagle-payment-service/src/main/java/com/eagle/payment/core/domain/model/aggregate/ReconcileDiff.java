package com.eagle.payment.core.domain.model.aggregate;

import com.eagle.datajpa.base.BaseAggregateRoot;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.ReconcileDiffType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 对账差异记录 (聚合根)。
 *
 * <p>每日对账任务扫描本地 Payment 与渠道清算单,发现差异时入此表,由运维 / 人工跟进。
 * 差异处理状态: 未处理 / 已处理 / 已忽略 (由 {@link #resolved} 标记)。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "t_reconcile_diff", comment = "对账差异",
        indexes = {
                @Index(name = "idx_diff_bill_date",
                        columnList = "bill_date, channel, resolved"),
                @Index(name = "idx_diff_out_trade_no", columnList = "channel, out_trade_no")
        })
public class ReconcileDiff extends BaseAggregateRoot<ReconcileDiff> {

    @Column(name = "bill_date", nullable = false, updatable = false,
            comment = "清算单日期")
    private LocalDate billDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, updatable = false, length = 16,
            comment = "渠道")
    private PaymentChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "diff_type", nullable = false, length = 32, comment = "差异类型")
    private ReconcileDiffType diffType;

    @Column(name = "out_trade_no", length = 64, comment = "渠道交易号")
    private String outTradeNo;

    @Column(name = "local_payment_id", comment = "本地 Payment ID (若有)")
    private Long localPaymentId;

    @Column(name = "local_amount", precision = 18, scale = 2, comment = "本地金额")
    private BigDecimal localAmount;

    @Column(name = "channel_amount", precision = 18, scale = 2, comment = "渠道清算金额")
    private BigDecimal channelAmount;

    @Column(name = "local_status", length = 16, comment = "本地状态")
    private String localStatus;

    @Column(name = "channel_status", length = 32, comment = "渠道状态")
    private String channelStatus;

    @Column(name = "resolved", nullable = false, comment = "是否已处理")
    private boolean resolved;

    @Column(name = "remark", length = 512, comment = "处理备注")
    private String remark;

    private ReconcileDiff(LocalDate billDate, PaymentChannel channel, ReconcileDiffType diffType,
                          @Nullable String outTradeNo, @Nullable Long localPaymentId,
                          @Nullable BigDecimal localAmount, @Nullable BigDecimal channelAmount,
                          @Nullable String localStatus, @Nullable String channelStatus) {
        this.billDate = billDate;
        this.channel = channel;
        this.diffType = diffType;
        this.outTradeNo = outTradeNo;
        this.localPaymentId = localPaymentId;
        this.localAmount = localAmount;
        this.channelAmount = channelAmount;
        this.localStatus = localStatus;
        this.channelStatus = channelStatus;
        this.resolved = false;
    }

    public static ReconcileDiff localMissing(LocalDate billDate, PaymentChannel channel,
                                             String outTradeNo, BigDecimal channelAmount,
                                             String channelStatus) {
        return new ReconcileDiff(billDate, channel, ReconcileDiffType.LOCAL_MISSING,
                outTradeNo, null, null, channelAmount, null, channelStatus);
    }

    public static ReconcileDiff channelMissing(LocalDate billDate, PaymentChannel channel,
                                               String outTradeNo, Long localPaymentId,
                                               BigDecimal localAmount, String localStatus) {
        return new ReconcileDiff(billDate, channel, ReconcileDiffType.CHANNEL_MISSING,
                outTradeNo, localPaymentId, localAmount, null, localStatus, null);
    }

    public static ReconcileDiff amountMismatch(LocalDate billDate, PaymentChannel channel,
                                               String outTradeNo, Long localPaymentId,
                                               BigDecimal localAmount, BigDecimal channelAmount) {
        return new ReconcileDiff(billDate, channel, ReconcileDiffType.AMOUNT_MISMATCH,
                outTradeNo, localPaymentId, localAmount, channelAmount, null, null);
    }

    public static ReconcileDiff statusMismatch(LocalDate billDate, PaymentChannel channel,
                                               String outTradeNo, Long localPaymentId,
                                               String localStatus, String channelStatus) {
        return new ReconcileDiff(billDate, channel, ReconcileDiffType.STATUS_MISMATCH,
                outTradeNo, localPaymentId, null, null, localStatus, channelStatus);
    }

    public void resolve(String remark) {
        this.resolved = true;
        this.remark = remark;
    }
}
