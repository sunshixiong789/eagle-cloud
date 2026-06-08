package com.eagle.payment.core.application.service;

import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.port.GatewayNotifyResult;
import com.eagle.payment.core.domain.repository.PaymentRepository;
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
    @InjectMocks
    private PaymentNotifyApplicationService service;

    private Payment ofStatus(PaymentStatus initial) {
        Payment p = Payment.create("t1", "ORD-001", PaymentChannel.ALIPAY, PaymentScene.PC_WEB,
                new BigDecimal("99.00"), "CNY", "subject", null,
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
}
