package com.eagle.payment.core.domain.model.aggregate;

import com.eagle.common.exception.DomainException;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Refund 聚合根状态机")
class RefundTest {

    private Refund create() {
        return Refund.create(1024L, "REF-001",
                PaymentChannel.ALIPAY, new BigDecimal("30.00"), "user-cancel");
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("初始状态应为 PENDING")
        void shouldStartAtPending() {
            Refund r = create();
            assertThat(r.getStatus()).isEqualTo(RefundStatus.PENDING);
        }

        @Test
        @DisplayName("金额 <= 0 应抛 DomainException")
        void shouldRejectInvalidAmount() {
            assertThatThrownBy(() -> Refund.create(1024L, "REF-001",
                    PaymentChannel.ALIPAY, BigDecimal.ZERO, null))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("submittedToChannel")
    class SubmittedToChannel {

        @Test
        @DisplayName("PENDING → REFUNDING 并回填 channelRefundNo")
        void shouldTransitionToRefunding() {
            Refund r = create();
            r.submittedToChannel("CHAN-REF-001");
            assertThat(r.getStatus()).isEqualTo(RefundStatus.REFUNDING);
            assertThat(r.getChannelRefundNo()).isEqualTo("CHAN-REF-001");
        }

        @Test
        @DisplayName("非 PENDING 状态再次提交应抛 DomainException")
        void shouldRejectNonPending() {
            Refund r = create();
            r.submittedToChannel("CHAN-1");
            assertThatThrownBy(() -> r.submittedToChannel("CHAN-2"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("markRefunded")
    class MarkRefunded {

        @Test
        @DisplayName("PENDING / REFUNDING → REFUNDED 并设置 refundedAt")
        void shouldTransitionToRefunded() {
            Refund r = create();
            r.submittedToChannel("CHAN-1");
            LocalDateTime ts = LocalDateTime.now();
            r.markRefunded(ts, "CHAN-1");
            assertThat(r.getStatus()).isEqualTo(RefundStatus.REFUNDED);
            assertThat(r.getRefundedAt()).isEqualTo(ts);
            assertThat(r.getChannelRefundNo()).isEqualTo("CHAN-1");
        }

        @Test
        @DisplayName("已 REFUNDED 再次回调应幂等不变")
        void shouldBeIdempotentOnSecondRefunded() {
            Refund r = create();
            r.submittedToChannel("CHAN-1");
            LocalDateTime first = LocalDateTime.now();
            r.markRefunded(first, "CHAN-1");
            r.markRefunded(LocalDateTime.now().plusMinutes(1), "CHAN-1");
            assertThat(r.getRefundedAt()).isEqualTo(first);
        }

        @Test
        @DisplayName("FAILED 状态再 markRefunded 应抛 DomainException")
        void shouldRejectAfterFailed() {
            Refund r = create();
            r.markFailed("network error");
            assertThatThrownBy(() -> r.markRefunded(LocalDateTime.now(), "CHAN-1"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("markFailed")
    class MarkFailed {

        @Test
        @DisplayName("PENDING → FAILED 并记录 reason")
        void shouldTransitionToFailed() {
            Refund r = create();
            r.markFailed("channel error");
            assertThat(r.getStatus()).isEqualTo(RefundStatus.FAILED);
            assertThat(r.getFailReason()).isEqualTo("channel error");
        }

        @Test
        @DisplayName("REFUNDED 状态再 markFailed 应抛 DomainException")
        void shouldRejectAfterRefunded() {
            Refund r = create();
            r.submittedToChannel("CHAN-1");
            r.markRefunded(LocalDateTime.now(), "CHAN-1");
            assertThatThrownBy(() -> r.markFailed("late fail"))
                    .isInstanceOf(DomainException.class);
        }
    }
}
