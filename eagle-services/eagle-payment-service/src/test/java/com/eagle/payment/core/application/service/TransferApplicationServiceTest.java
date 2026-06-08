package com.eagle.payment.core.application.service;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.payment.core.domain.model.aggregate.Transfer;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.TransferMode;
import com.eagle.payment.core.domain.model.enums.TransferStatus;
import com.eagle.payment.core.domain.port.GatewayTransferCommand;
import com.eagle.payment.core.domain.port.GatewayTransferResult;
import com.eagle.payment.core.domain.port.PaymentGatewayPort;
import com.eagle.payment.core.domain.repository.TransferRepository;
import com.eagle.payment.core.infrastructure.config.PaymentProperties;
import com.eagle.payment.core.interfaces.dto.request.CreateTransferRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferApplicationService")
class TransferApplicationServiceTest {

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private PaymentGatewayPort alipayGateway;

    private PaymentProperties properties;
    private TransferApplicationService service;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.getTransfer().setEnabled(true);
        properties.getTransfer().setSingleAmountLimit(5000L);
        properties.getTransfer().setDailyAmountLimit(50000L);
        properties.getTransfer().setDailyCountLimit(20);
        lenient().when(alipayGateway.getChannel()).thenReturn(PaymentChannel.ALIPAY);
        service = new TransferApplicationService(transferRepository, properties,
                List.of(alipayGateway));
    }

    private CreateTransferRequest request(BigDecimal amount) {
        CreateTransferRequest req = new CreateTransferRequest();
        req.setBizTransferNo("TRN-001");
        req.setMode(TransferMode.IMMEDIATE);
        req.setChannel(PaymentChannel.ALIPAY);
        req.setRecipientAccount("user@example.com");
        req.setRecipientName("张三");
        req.setAmount(amount);
        req.setReason("月度结算");
        return req;
    }

    private void stubRiskControlOk() {
        when(transferRepository.sumAmountInPeriod(anyList(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transferRepository.countInPeriod(anyList(), any(), any()))
                .thenReturn(0L);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("提现总开关关闭应抛 TRANSFER_DISABLED")
        void shouldRejectWhenDisabled() {
            properties.getTransfer().setEnabled(false);
            assertThatThrownBy(() -> service.create(request(new BigDecimal("500.00"))))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("支付宝同步成功: 应保存 SUCCESS")
        void shouldCompleteSyncForAlipay() {
            stubRiskControlOk();
            when(transferRepository.existsByBizTransferNo(eq("TRN-001")))
                    .thenReturn(false);
            when(transferRepository.saveAndFlush(any(Transfer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(transferRepository.save(any(Transfer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(alipayGateway.transfer(any(GatewayTransferCommand.class)))
                    .thenReturn(new GatewayTransferResult("CHAN-T-1",
                            TransferStatus.SUCCESS, LocalDateTime.now(), null));

            Transfer t = service.create(request(new BigDecimal("500.00")));

            assertThat(t.getStatus()).isEqualTo(TransferStatus.SUCCESS);
            assertThat(t.getChannelTransferNo()).isEqualTo("CHAN-T-1");
        }

        @Test
        @DisplayName("超过单笔限额抛 EXCEED_SINGLE_LIMIT")
        void shouldRejectOverSingleLimit() {
            assertThatThrownBy(() -> service.create(request(new BigDecimal("6000.00"))))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("超过日累计金额抛 EXCEED_DAILY_AMOUNT")
        void shouldRejectOverDailyAmount() {
            when(transferRepository.sumAmountInPeriod(anyList(), any(), any()))
                    .thenReturn(new BigDecimal("49600.00"));
            assertThatThrownBy(() -> service.create(request(new BigDecimal("500.00"))))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("超过日笔数抛 EXCEED_DAILY_COUNT")
        void shouldRejectOverDailyCount() {
            when(transferRepository.sumAmountInPeriod(anyList(), any(), any()))
                    .thenReturn(BigDecimal.ZERO);
            when(transferRepository.countInPeriod(anyList(), any(), any()))
                    .thenReturn(20L);
            assertThatThrownBy(() -> service.create(request(new BigDecimal("500.00"))))
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("bizTransferNo 重复抛 ConflictException")
        void shouldRejectDuplicate() {
            stubRiskControlOk();
            when(transferRepository.existsByBizTransferNo(eq("TRN-001")))
                    .thenReturn(true);
            assertThatThrownBy(() -> service.create(request(new BigDecimal("500.00"))))
                    .isInstanceOf(ConflictException.class);
            verify(transferRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("APPROVAL 模式 create 应停在 PENDING_APPROVAL,不调渠道")
        void shouldStopAtPendingApprovalForApprovalMode() {
            stubRiskControlOk();
            when(transferRepository.existsByBizTransferNo(eq("TRN-001"))).thenReturn(false);
            when(transferRepository.saveAndFlush(any(Transfer.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CreateTransferRequest req = request(new BigDecimal("500.00"));
            req.setMode(TransferMode.APPROVAL);

            Transfer result = service.create(req);

            assertThat(result.getStatus()).isEqualTo(TransferStatus.PENDING_APPROVAL);
            assertThat(result.getMode()).isEqualTo(TransferMode.APPROVAL);
            verify(alipayGateway, never()).transfer(any());
        }
    }
}
