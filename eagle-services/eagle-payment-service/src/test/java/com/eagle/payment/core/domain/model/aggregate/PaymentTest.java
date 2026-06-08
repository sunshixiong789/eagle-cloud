package com.eagle.payment.core.domain.model.aggregate;

import com.eagle.common.exception.DomainException;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment 聚合根状态机")
class PaymentTest {

    private static final String BIZ_ORDER_NO = "ORD-001";
    private static final BigDecimal AMOUNT = new BigDecimal("99.00");

    private Payment createPayment() {
        return Payment.create(BIZ_ORDER_NO, PaymentChannel.ALIPAY, PaymentScene.PC_WEB,
                AMOUNT, "CNY", "Subject", "u1",
                LocalDateTime.now().plusMinutes(30));
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("初始状态应为 CREATED 且 refundedAmount = 0")
        void shouldStartAtCreated() {
            Payment p = createPayment();
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.CREATED);
            assertThat(p.getRefundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("金额 <= 0 应抛 DomainException")
        void shouldRejectInvalidAmount() {
            assertThatThrownBy(() -> Payment.create(BIZ_ORDER_NO, PaymentChannel.ALIPAY,
                    PaymentScene.PC_WEB, BigDecimal.ZERO, "CNY", "subj", null,
                    LocalDateTime.now().plusMinutes(30)))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("submittedToChannel")
    class SubmittedToChannel {

        @Test
        @DisplayName("CREATED → PAYING 并回填 outTradeNo")
        void shouldTransitionToPaying() {
            Payment p = createPayment();
            p.submittedToChannel("OUT-001");
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAYING);
            assertThat(p.getOutTradeNo()).isEqualTo("OUT-001");
        }

        @Test
        @DisplayName("非 CREATED 状态再次提交应抛 DomainException")
        void shouldRejectNonCreated() {
            Payment p = createPayment();
            p.submittedToChannel("OUT-001");
            assertThatThrownBy(() -> p.submittedToChannel("OUT-002"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("markPaid")
    class MarkPaid {

        @Test
        @DisplayName("PAYING → PAID 并设置 paidAt")
        void shouldTransitionToPaid() {
            Payment p = createPayment();
            p.submittedToChannel("OUT-001");
            LocalDateTime paidAt = LocalDateTime.now();
            p.markPaid(paidAt, "CHAN-001");
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(p.getPaidAt()).isEqualTo(paidAt);
            assertThat(p.getOutTradeNo()).isEqualTo("CHAN-001");
        }

        @Test
        @DisplayName("已 PAID 再次回调应幂等不变")
        void shouldBeIdempotentOnSecondPaid() {
            Payment p = createPayment();
            p.submittedToChannel("OUT-001");
            LocalDateTime first = LocalDateTime.now();
            p.markPaid(first, "CHAN-001");
            // 二次回调不抛异常,paidAt 保持
            p.markPaid(LocalDateTime.now().plusMinutes(1), "CHAN-001");
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(p.getPaidAt()).isEqualTo(first);
        }

        @Test
        @DisplayName("CANCELLED / EXPIRED 状态再 markPaid 应抛 DomainException")
        void shouldRejectTerminalNonPaid() {
            Payment p = createPayment();
            p.cancel("user cancelled");
            assertThatThrownBy(() -> p.markPaid(LocalDateTime.now(), "CHAN"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("CREATED → CANCELLED 应记录 reason")
        void shouldCancelFromCreated() {
            Payment p = createPayment();
            p.cancel("user");
            assertThat(p.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(p.getFailReason()).isEqualTo("user");
        }

        @Test
        @DisplayName("PAID 状态不允许 cancel")
        void shouldRejectCancelAfterPaid() {
            Payment p = createPayment();
            p.submittedToChannel("OUT-001");
            p.markPaid(LocalDateTime.now(), "CHAN-001");
            assertThatThrownBy(() -> p.cancel("late"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("accumulateRefund")
    class AccumulateRefund {

        @Test
        @DisplayName("退款累加成功后 refundableAmount 应减少")
        void shouldAccumulate() {
            Payment p = createPayment();
            p.submittedToChannel("OUT-001");
            p.markPaid(LocalDateTime.now(), "CHAN-001");
            p.accumulateRefund(new BigDecimal("30.00"));
            assertThat(p.getRefundedAmount()).isEqualByComparingTo("30.00");
            assertThat(p.refundableAmount()).isEqualByComparingTo("69.00");
        }

        @Test
        @DisplayName("累计退款超过订单金额应抛 DomainException")
        void shouldRejectOverRefund() {
            Payment p = createPayment();
            p.submittedToChannel("OUT-001");
            p.markPaid(LocalDateTime.now(), "CHAN-001");
            p.accumulateRefund(new BigDecimal("80.00"));
            assertThatThrownBy(() -> p.accumulateRefund(new BigDecimal("20.01")))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("非 PAID 状态退款应抛 DomainException")
        void shouldRejectRefundWhenNotPaid() {
            Payment p = createPayment();
            assertThatThrownBy(() -> p.accumulateRefund(BigDecimal.TEN))
                    .isInstanceOf(DomainException.class);
        }
    }
}
