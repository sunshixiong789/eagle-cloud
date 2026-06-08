package com.eagle.payment.core.application.service;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.aggregate.Refund;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.model.enums.RefundStatus;
import com.eagle.payment.core.domain.port.GatewayRefundCommand;
import com.eagle.payment.core.domain.port.GatewayRefundResult;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import com.eagle.payment.core.domain.repository.RefundRepository;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import com.eagle.payment.core.interfaces.dto.request.CreateRefundRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundApplicationService")
class RefundApplicationServiceTest {

    @Mock
    private RefundRepository refundRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentGatewayPort alipayGateway;

    private PaymentProperties properties;
    private RefundApplicationService service;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        when(alipayGateway.getChannel()).thenReturn(PaymentChannel.ALIPAY);
        service = new RefundApplicationService(refundRepository, paymentRepository,
                properties, List.of(alipayGateway));
    }

    private Payment paidPayment(BigDecimal amount, BigDecimal alreadyRefunded) {
        Payment p = Payment.create("default", "ORD-001", PaymentChannel.ALIPAY,
                PaymentScene.PC_WEB, amount, "CNY", "subject", null,
                LocalDateTime.now().plusMinutes(30));
        p.submittedToChannel("OUT-001");
        p.markPaid(LocalDateTime.now(), "OUT-001");
        if (alreadyRefunded.signum() > 0) {
            p.accumulateRefund(alreadyRefunded);
        }
        return p;
    }

    private CreateRefundRequest request(BigDecimal amount) {
        CreateRefundRequest req = new CreateRefundRequest();
        req.setPaymentId(1024L);
        req.setBizRefundNo("REF-001");
        req.setAmount(amount);
        req.setReason("user cancelled");
        return req;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("支付宝同步成功: 应保存 REFUNDED 并累加 Payment.refundedAmount")
        void shouldCompleteSyncForAlipay() {
            when(refundRepository.existsByTenantIdAndBizRefundNo(anyString(), eq("REF-001")))
                    .thenReturn(false);
            Payment payment = paidPayment(new BigDecimal("99.00"), BigDecimal.ZERO);
            when(paymentRepository.findById(1024L)).thenReturn(Optional.of(payment));
            when(refundRepository.saveAndFlush(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(alipayGateway.refund(any(GatewayRefundCommand.class)))
                    .thenReturn(new GatewayRefundResult("CHAN-REF-1",
                            RefundStatus.REFUNDED, LocalDateTime.now(), null));

            Refund refund = service.create(request(new BigDecimal("30.00")));

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUNDED);
            assertThat(refund.getChannelRefundNo()).isEqualTo("CHAN-REF-1");
            assertThat(payment.getRefundedAmount()).isEqualByComparingTo("30.00");
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("微信异步处理: REFUNDING 状态保存,不累加 Payment.refundedAmount")
        void shouldPersistRefundingForWechat() {
            when(refundRepository.existsByTenantIdAndBizRefundNo(anyString(), eq("REF-001")))
                    .thenReturn(false);
            Payment payment = paidPayment(new BigDecimal("99.00"), BigDecimal.ZERO);
            when(paymentRepository.findById(1024L)).thenReturn(Optional.of(payment));
            when(refundRepository.saveAndFlush(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(alipayGateway.refund(any(GatewayRefundCommand.class)))
                    .thenReturn(new GatewayRefundResult("CHAN-REF-1",
                            RefundStatus.REFUNDING, null, null));

            Refund refund = service.create(request(new BigDecimal("30.00")));

            assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUNDING);
            assertThat(payment.getRefundedAmount()).isEqualByComparingTo("0.00");
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Payment 不存在抛 NotFoundException")
        void shouldRejectMissingPayment() {
            when(refundRepository.existsByTenantIdAndBizRefundNo(anyString(), eq("REF-001")))
                    .thenReturn(false);
            when(paymentRepository.findById(1024L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(request(new BigDecimal("30.00"))))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("Payment 非 PAID 抛 DomainException (PAYMENT_NOT_PAID)")
        void shouldRejectNonPaidPayment() {
            when(refundRepository.existsByTenantIdAndBizRefundNo(anyString(), eq("REF-001")))
                    .thenReturn(false);
            Payment p = Payment.create("default", "ORD-001", PaymentChannel.ALIPAY,
                    PaymentScene.PC_WEB, new BigDecimal("99.00"), "CNY", "subj", null,
                    LocalDateTime.now().plusMinutes(30));
            when(paymentRepository.findById(1024L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.create(request(new BigDecimal("30.00"))))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("退款超过可退余额抛 DomainException (EXCEED_REFUNDABLE)")
        void shouldRejectExceedRefundable() {
            when(refundRepository.existsByTenantIdAndBizRefundNo(anyString(), eq("REF-001")))
                    .thenReturn(false);
            Payment payment = paidPayment(new BigDecimal("99.00"), new BigDecimal("80.00"));
            when(paymentRepository.findById(1024L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.create(request(new BigDecimal("30.00"))))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("bizRefundNo 重复抛 ConflictException")
        void shouldRejectDuplicate() {
            when(refundRepository.existsByTenantIdAndBizRefundNo(anyString(), eq("REF-001")))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.create(request(new BigDecimal("30.00"))))
                    .isInstanceOf(ConflictException.class);
            verify(paymentRepository, never()).findById(any());
        }

        @Test
        @DisplayName("关闭部分退款时,非全额退款抛 PARTIAL_DISABLED")
        void shouldRejectPartialWhenDisabled() {
            properties.getRefund().setAllowPartial(false);
            when(refundRepository.existsByTenantIdAndBizRefundNo(anyString(), eq("REF-001")))
                    .thenReturn(false);
            Payment payment = paidPayment(new BigDecimal("99.00"), BigDecimal.ZERO);
            when(paymentRepository.findById(1024L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> service.create(request(new BigDecimal("30.00"))))
                    .isInstanceOf(DomainException.class);
        }
    }
}
