package com.eagle.payment.core.domain.model.aggregate;

import com.eagle.common.exception.DomainException;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Transfer 聚合根状态机")
class TransferTest {

    private static final Long USER_ID = 100086L;

    private Transfer create() {
        return Transfer.create(USER_ID, "TRN-001", TransferMode.IMMEDIATE, PaymentChannel.ALIPAY,
                "user@example.com", "张三", new BigDecimal("500.00"), "月度结算");
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("初始状态应为 PENDING")
        void shouldStartAtPending() {
            assertThat(create().getStatus()).isEqualTo(TransferStatus.PENDING);
        }

        @Test
        @DisplayName("金额 <= 0 应抛 DomainException")
        void shouldRejectInvalidAmount() {
            assertThatThrownBy(() -> Transfer.create(USER_ID, "TRN-001", TransferMode.IMMEDIATE,
                    PaymentChannel.ALIPAY, "user", null, BigDecimal.ZERO, null))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("IMMEDIATE 模式初始状态应为 PENDING")
        void shouldStartAtPendingForImmediate() {
            Transfer t = Transfer.create(USER_ID, "TRN-001", TransferMode.IMMEDIATE,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "月度结算");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.PENDING);
            assertThat(t.getMode()).isEqualTo(TransferMode.IMMEDIATE);
        }

        @Test
        @DisplayName("APPROVAL 模式初始状态应为 PENDING_APPROVAL")
        void shouldStartAtPendingApprovalForApproval() {
            Transfer t = Transfer.create(USER_ID, "TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "月度结算");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
            assertThat(t.getMode()).isEqualTo(TransferMode.APPROVAL);
        }
    }

    @Nested
    @DisplayName("submittedToChannel + markSucceeded")
    class HappyPath {

        @Test
        @DisplayName("PENDING → SUBMITTED → SUCCESS")
        void shouldFlowToSuccess() {
            Transfer t = create();
            t.submittedToChannel("CHAN-T-1");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.SUBMITTED);
            LocalDateTime ts = LocalDateTime.now();
            t.markSucceeded(ts, "CHAN-T-1");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.SUCCESS);
            assertThat(t.getSucceededAt()).isEqualTo(ts);
        }

        @Test
        @DisplayName("已 SUCCESS 再次 markSucceeded 应幂等")
        void shouldBeIdempotent() {
            Transfer t = create();
            t.submittedToChannel("CHAN-T-1");
            LocalDateTime first = LocalDateTime.now();
            t.markSucceeded(first, "CHAN-T-1");
            t.markSucceeded(LocalDateTime.now().plusMinutes(1), "CHAN-T-1");
            assertThat(t.getSucceededAt()).isEqualTo(first);
        }
    }

    @Nested
    @DisplayName("markFailed")
    class Failed {

        @Test
        @DisplayName("PENDING → FAILED")
        void shouldFail() {
            Transfer t = create();
            t.markFailed("channel rejected");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.FAILED);
            assertThat(t.getFailReason()).isEqualTo("channel rejected");
        }

        @Test
        @DisplayName("SUCCESS 状态再 markFailed 应抛 DomainException")
        void shouldRejectAfterSuccess() {
            Transfer t = create();
            t.submittedToChannel("CHAN-T-1");
            t.markSucceeded(LocalDateTime.now(), "CHAN-T-1");
            assertThatThrownBy(() -> t.markFailed("late fail"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("markReturned (退票)")
    class Returned {

        @Test
        @DisplayName("SUCCESS → RETURNED")
        void shouldTransitionToReturned() {
            Transfer t = create();
            t.submittedToChannel("CHAN-T-1");
            t.markSucceeded(LocalDateTime.now(), "CHAN-T-1");
            t.markReturned("recipient account closed");
            assertThat(t.getStatus()).isEqualTo(TransferStatus.RETURNED);
        }

        @Test
        @DisplayName("PENDING 状态 markReturned 应抛 DomainException")
        void shouldRejectFromPending() {
            Transfer t = create();
            assertThatThrownBy(() -> t.markReturned("invalid"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("APPROVAL 模式 PENDING_APPROVAL → PENDING + 记录 approverId/approvedAt")
        void shouldTransitionToPendingAndRecordApprover() {
            Transfer t = Transfer.create(USER_ID, "TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            t.approve("admin-1");

            assertThat(t.getStatus()).isEqualTo(TransferStatus.PENDING);
            assertThat(t.getApproverId()).isEqualTo("admin-1");
            assertThat(t.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("IMMEDIATE 模式 approve 抛 NOT_APPROVAL_MODE")
        void shouldRejectApproveOnImmediate() {
            Transfer t = Transfer.create(USER_ID, "TRN-001", TransferMode.IMMEDIATE,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            assertThatThrownBy(() -> t.approve("admin-1"))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("非 PENDING_APPROVAL 状态 approve 抛 APPROVAL_NOT_ALLOWED_IN_STATUS")
        void shouldRejectApproveWhenStatusNotPendingApproval() {
            Transfer t = Transfer.create(USER_ID, "TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");
            t.approve("admin-1");

            assertThatThrownBy(() -> t.approve("admin-2"))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        @DisplayName("APPROVAL 模式 PENDING_APPROVAL → REJECTED + 记录原因")
        void shouldTransitionToRejected() {
            Transfer t = Transfer.create(USER_ID, "TRN-001", TransferMode.APPROVAL,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            t.reject("admin-1", "金额可疑");

            assertThat(t.getStatus()).isEqualTo(TransferStatus.REJECTED);
            assertThat(t.getApproverId()).isEqualTo("admin-1");
            assertThat(t.getRejectReason()).isEqualTo("金额可疑");
            assertThat(t.getRejectedAt()).isNotNull();
        }

        @Test
        @DisplayName("非 PENDING_APPROVAL 状态 reject 应抛 DomainException")
        void shouldRejectRejectWhenStatusNotPendingApproval() {
            Transfer t = Transfer.create(USER_ID, "TRN-001", TransferMode.IMMEDIATE,
                    PaymentChannel.ALIPAY, "user@example.com", "张三",
                    new BigDecimal("500.00"), "结算");

            assertThatThrownBy(() -> t.reject("admin-1", "any"))
                    .isInstanceOf(DomainException.class);
        }
    }
}
