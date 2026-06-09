package com.eagle.payment.core.application.service;

import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.aggregate.Refund;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.model.enums.RefundStatus;
import com.eagle.payment.core.domain.port.GatewayNotifyResult;
import com.eagle.payment.core.domain.port.GatewayRefundNotifyResult;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import com.eagle.payment.core.domain.repository.RefundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentNotifyApplicationService")
class PaymentNotifyApplicationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RefundRepository refundRepository;
    @InjectMocks
    private PaymentNotifyApplicationService service;

    private Payment ofStatus(PaymentStatus initial) {
        Payment p = Payment.create("ORD-001", PaymentChannel.ALIPAY, PaymentScene.PC_WEB,
                new BigDecimal("99.00"), "CNY", "subject", 100086L,
                LocalDateTime.now().plusMinutes(30));
        if (initial == PaymentStatus.PAYING) {
            p.submittedToChannel("OUT-001");
        }
        return p;
    }

    private GatewayNotifyResult notifyOf(boolean signValid, String outTradeNo,
                                         PaymentStatus status, BigDecimal amount) {
        return new GatewayNotifyResult(signValid, outTradeNo, "CHAN-001",
                status, amount, LocalDateTime.now(), null, "raw", "evt-1");
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("签名无效返回 SIGNATURE_INVALID")
        void shouldReturnSignatureInvalid() {
            GatewayNotifyResult result = GatewayNotifyResult.invalid("raw");
            var outcome = service.handle(PaymentChannel.ALIPAY, result);
            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.SIGNATURE_INVALID);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("outTradeNo 在本地不存在返回 UNKNOWN_PAYMENT")
        void shouldReturnUnknownPayment() {
            when(paymentRepository.findByChannelAndOutTradeNo(eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.empty());
            var result = notifyOf(true, "OUT-001", PaymentStatus.PAID, new BigDecimal("99.00"));
            var outcome = service.handle(PaymentChannel.ALIPAY, result);
            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.UNKNOWN_PAYMENT);
        }

        @Test
        @DisplayName("PAID 推进 Payment 到 PAID 并保存")
        void shouldMarkPaid() {
            Payment payment = ofStatus(PaymentStatus.PAYING);
            when(paymentRepository.findByChannelAndOutTradeNo(eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.of(payment));
            var result = notifyOf(true, "OUT-001", PaymentStatus.PAID, new BigDecimal("99.00"));
            var outcome = service.handle(PaymentChannel.ALIPAY, result);

            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.PROCESSED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("金额不一致返回 AMOUNT_MISMATCH 且不推进状态")
        void shouldRejectAmountMismatch() {
            Payment payment = ofStatus(PaymentStatus.PAYING);
            when(paymentRepository.findByChannelAndOutTradeNo(eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.of(payment));
            var result = notifyOf(true, "OUT-001", PaymentStatus.PAID, new BigDecimal("50.00"));
            var outcome = service.handle(PaymentChannel.ALIPAY, result);

            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.AMOUNT_MISMATCH);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYING);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("FAILED 推进 Payment 到 FAILED 并保存")
        void shouldMarkFailed() {
            Payment payment = ofStatus(PaymentStatus.PAYING);
            when(paymentRepository.findByChannelAndOutTradeNo(eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.of(payment));
            GatewayNotifyResult result = new GatewayNotifyResult(true, "OUT-001", "CHAN-001",
                    PaymentStatus.FAILED, new BigDecimal("99.00"), null,
                    "trade closed", "raw", null);
            var outcome = service.handle(PaymentChannel.ALIPAY, result);

            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.PROCESSED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.getFailReason()).isEqualTo("trade closed");
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("PAYING 非终态状态返回 NON_TERMINAL")
        void shouldReturnNonTerminal() {
            Payment payment = ofStatus(PaymentStatus.PAYING);
            when(paymentRepository.findByChannelAndOutTradeNo(eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.of(payment));
            var result = notifyOf(true, "OUT-001", PaymentStatus.PAYING, new BigDecimal("99.00"));
            var outcome = service.handle(PaymentChannel.ALIPAY, result);

            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.NON_TERMINAL);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAYING);
        }
    }

    @Nested
    @DisplayName("handleRefund")
    class HandleRefund {

        private Payment paid() {
            Payment p = Payment.create("ORD-001", PaymentChannel.ALIPAY,
                    PaymentScene.PC_WEB, new BigDecimal("99.00"), "CNY", "subject", 100086L,
                    LocalDateTime.now().plusMinutes(30));
            p.submittedToChannel("OUT-001");
            p.markPaid(LocalDateTime.now(), "OUT-001");
            return p;
        }

        private Refund refunding() {
            Refund r = Refund.create(1024L, 100086L, "REF-001",
                    PaymentChannel.ALIPAY, new BigDecimal("30.00"), null);
            r.submittedToChannel("CHAN-REF-1");
            return r;
        }

        @Test
        @DisplayName("签名无效返回 SIGNATURE_INVALID")
        void shouldReturnSignatureInvalid() {
            GatewayRefundNotifyResult result = GatewayRefundNotifyResult.invalid("raw");
            var outcome = service.handleRefund(PaymentChannel.ALIPAY, result);
            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.SIGNATURE_INVALID);
            verify(refundRepository, never()).save(any());
        }

        @Test
        @DisplayName("refundNo 在本地不存在返回 UNKNOWN_PAYMENT")
        void shouldReturnUnknownRefund() {
            when(refundRepository.findByChannelAndChannelRefundNo(
                    eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.empty());
            when(refundRepository.findByBizRefundNo(anyString()))
                    .thenReturn(Optional.empty());
            GatewayRefundNotifyResult result = new GatewayRefundNotifyResult(true,
                    "REF-001", "CHAN-REF-1", RefundStatus.REFUNDED,
                    new BigDecimal("30.00"), LocalDateTime.now(), null, "raw");
            var outcome = service.handleRefund(PaymentChannel.ALIPAY, result);
            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.UNKNOWN_PAYMENT);
        }

        @Test
        @DisplayName("REFUNDED 推进 Refund 并累加 Payment.refundedAmount")
        void shouldMarkRefundedAndAccumulate() {
            Refund refund = refunding();
            when(refundRepository.findByChannelAndChannelRefundNo(
                    eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.of(refund));
            Payment payment = paid();
            when(paymentRepository.findById(1024L)).thenReturn(Optional.of(payment));
            GatewayRefundNotifyResult result = new GatewayRefundNotifyResult(true,
                    "REF-001", "CHAN-REF-1", RefundStatus.REFUNDED,
                    new BigDecimal("30.00"), LocalDateTime.now(), null, "raw");

            var outcome = service.handleRefund(PaymentChannel.ALIPAY, result);

            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.PROCESSED);
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUNDED);
            assertThat(payment.getRefundedAmount()).isEqualByComparingTo("30.00");
            verify(refundRepository).save(refund);
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("FAILED 推进 Refund 但不动 Payment")
        void shouldMarkFailedWithoutTouchingPayment() {
            Refund refund = refunding();
            when(refundRepository.findByChannelAndChannelRefundNo(
                    eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.of(refund));
            GatewayRefundNotifyResult result = new GatewayRefundNotifyResult(true,
                    "REF-001", "CHAN-REF-1", RefundStatus.FAILED,
                    new BigDecimal("30.00"), null, "channel fail", "raw");

            var outcome = service.handleRefund(PaymentChannel.ALIPAY, result);

            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.PROCESSED);
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
            verify(refundRepository).save(refund);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("金额不一致返回 AMOUNT_MISMATCH")
        void shouldRejectAmountMismatch() {
            Refund refund = refunding();
            when(refundRepository.findByChannelAndChannelRefundNo(
                    eq(PaymentChannel.ALIPAY), anyString()))
                    .thenReturn(Optional.of(refund));
            GatewayRefundNotifyResult result = new GatewayRefundNotifyResult(true,
                    "REF-001", "CHAN-REF-1", RefundStatus.REFUNDED,
                    new BigDecimal("50.00"), LocalDateTime.now(), null, "raw");

            var outcome = service.handleRefund(PaymentChannel.ALIPAY, result);

            assertThat(outcome).isEqualTo(
                    PaymentNotifyApplicationService.NotifyOutcome.AMOUNT_MISMATCH);
            verify(refundRepository, never()).save(any());
        }
    }
}
