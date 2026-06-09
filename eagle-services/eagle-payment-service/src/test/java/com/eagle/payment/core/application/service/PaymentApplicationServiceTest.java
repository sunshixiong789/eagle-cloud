package com.eagle.payment.core.application.service;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.port.GatewayPayCommand;
import com.eagle.payment.core.domain.port.GatewayPayResult;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import com.eagle.payment.core.interfaces.dto.request.CreatePaymentRequest;
import com.eagle.payment.core.interfaces.dto.response.CreatePaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentApplicationService")
class PaymentApplicationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private PaymentGatewayPort alipayGateway;

    private static final Long USER_ID = 100086L;

    private PaymentApplicationService service;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        when(alipayGateway.getChannel()).thenReturn(PaymentChannel.ALIPAY);
        service = new PaymentApplicationService(paymentRepository, idGenerator,
                properties, List.of(alipayGateway));
    }

    private CreatePaymentRequest request() {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setBizOrderNo("ORD-001");
        req.setChannel(PaymentChannel.ALIPAY);
        req.setScene(PaymentScene.PC_WEB);
        req.setAmount(new BigDecimal("99.00"));
        req.setCurrency("CNY");
        req.setSubject("subject");
        return req;
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("应保存 Payment 并提交到渠道后迁移到 PAYING")
        void shouldCreatePaymentAndSubmitToGateway() {
            when(paymentRepository.existsByBizOrderNoAndChannel(
                    eq("ORD-001"), eq(PaymentChannel.ALIPAY))).thenReturn(false);
            when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(idGenerator.nextIdStr()).thenReturn("16812340000001");
            when(alipayGateway.createPayment(any(GatewayPayCommand.class)))
                    .thenReturn(new GatewayPayResult(null, "<form>...</form>", "html-form"));

            CreatePaymentResponse resp = service.create(request(), USER_ID);

            assertThat(resp.outTradeNo()).isEqualTo("PAY16812340000001");
            assertThat(resp.payload()).isEqualTo("<form>...</form>");
            assertThat(resp.payloadType()).isEqualTo("html-form");

            ArgumentCaptor<GatewayPayCommand> cmd = ArgumentCaptor.forClass(GatewayPayCommand.class);
            verify(alipayGateway).createPayment(cmd.capture());
            assertThat(cmd.getValue().channel()).isEqualTo(PaymentChannel.ALIPAY);
            assertThat(cmd.getValue().outTradeNo()).isEqualTo("PAY16812340000001");

            ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(saved.capture());
            assertThat(saved.getValue().getStatus()).isEqualTo(PaymentStatus.PAYING);
            assertThat(saved.getValue().getOutTradeNo()).isEqualTo("PAY16812340000001");
        }

        @Test
        @DisplayName("bizOrderNo + channel 已存在应抛 ConflictException")
        void shouldRejectDuplicate() {
            when(paymentRepository.existsByBizOrderNoAndChannel(
                    eq("ORD-001"), eq(PaymentChannel.ALIPAY))).thenReturn(true);

            assertThatThrownBy(() -> service.create(request(), USER_ID))
                    .isInstanceOf(ConflictException.class);
            verify(paymentRepository, never()).saveAndFlush(any());
            verify(alipayGateway, never()).createPayment(any());
        }

        @Test
        @DisplayName("渠道未注册应抛 DomainException (CHANNEL_UNAVAILABLE)")
        void shouldRejectMissingChannel() {
            when(paymentRepository.existsByBizOrderNoAndChannel(
                    eq("ORD-001"), eq(PaymentChannel.WECHAT))).thenReturn(false);
            CreatePaymentRequest req = request();
            req.setChannel(PaymentChannel.WECHAT);

            assertThatThrownBy(() -> service.create(req, USER_ID))
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("查询归属校验")
    class OwnershipCheck {

        private Payment ownedBy(Long ownerId) {
            return Payment.create("ORD-001", PaymentChannel.ALIPAY, PaymentScene.PC_WEB,
                    new BigDecimal("99.00"), "CNY", "subject", ownerId,
                    java.time.LocalDateTime.now().plusMinutes(30));
        }

        @Test
        @DisplayName("findById: 他人订单按 NOT_FOUND 返回 (避免泄漏存在性)")
        void shouldHidePaymentOwnedByOthers() {
            when(paymentRepository.findById(1L)).thenReturn(java.util.Optional.of(ownedBy(999L)));

            assertThatThrownBy(() -> service.findById(1L, USER_ID))
                    .isInstanceOf(com.eagle.common.exception.NotFoundException.class);
        }

        @Test
        @DisplayName("findByBizOrderNo: 他人订单按 NOT_FOUND 返回")
        void shouldHidePaymentByBizOrderNoOwnedByOthers() {
            when(paymentRepository.findByBizOrderNoAndChannel("ORD-X", PaymentChannel.ALIPAY))
                    .thenReturn(java.util.Optional.of(ownedBy(999L)));

            assertThatThrownBy(() -> service.findByBizOrderNo("ORD-X", PaymentChannel.ALIPAY, USER_ID))
                    .isInstanceOf(com.eagle.common.exception.NotFoundException.class);
        }

        @Test
        @DisplayName("cancel: 他人订单按 NOT_FOUND 返回,且不会发生状态迁移")
        void shouldBlockCancelOnOthersPayment() {
            Payment foreign = ownedBy(999L);
            when(paymentRepository.findById(1L)).thenReturn(java.util.Optional.of(foreign));

            assertThatThrownBy(() -> service.cancel(1L, "abuse", USER_ID))
                    .isInstanceOf(com.eagle.common.exception.NotFoundException.class);
            assertThat(foreign.getStatus()).isEqualTo(PaymentStatus.CREATED);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("findById: 本人订单正常返回")
        void shouldReturnOwnedPayment() {
            when(paymentRepository.findById(1L)).thenReturn(java.util.Optional.of(ownedBy(USER_ID)));

            Payment p = service.findById(1L, USER_ID);
            assertThat(p.getUserId()).isEqualTo(USER_ID);
        }
    }
}
