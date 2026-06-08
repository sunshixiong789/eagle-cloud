package com.eagle.payment.core.application.service;

import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.aggregate.ReconcileDiff;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentScene;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.model.enums.ReconcileDiffType;
import com.eagle.payment.core.domain.port.BillEntry;
import com.eagle.payment.core.domain.port.ReconcileBillFetchPort;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import com.eagle.payment.core.domain.repository.ReconcileDiffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconcileApplicationService")
class ReconcileApplicationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReconcileDiffRepository diffRepository;
    @Mock
    private ReconcileBillFetchPort alipayFetcher;

    private ReconcileApplicationService service;

    private static final LocalDate BILL_DATE = LocalDate.of(2026, 6, 7);

    @BeforeEach
    void setUp() {
        when(alipayFetcher.supports(PaymentChannel.ALIPAY)).thenReturn(true);
        service = new ReconcileApplicationService(paymentRepository, diffRepository,
                List.of(alipayFetcher));
    }

    private Payment paid(String outTradeNo, BigDecimal amount, LocalDateTime paidAt) {
        Payment p = Payment.create("default", "ORD-" + outTradeNo, PaymentChannel.ALIPAY,
                PaymentScene.PC_WEB, amount, "CNY", "subject", null,
                paidAt.plusMinutes(30));
        p.submittedToChannel(outTradeNo);
        p.markPaid(paidAt, outTradeNo);
        return p;
    }

    @Nested
    @DisplayName("reconcile")
    class Reconcile {

        @Test
        @DisplayName("完全一致返回 0 差异,不写表")
        void shouldFindNoDiff() {
            LocalDateTime paidAt = LocalDateTime.of(BILL_DATE, java.time.LocalTime.of(10, 0));
            Payment p1 = paid("OUT-1", new BigDecimal("99.00"), paidAt);
            when(paymentRepository.findByStatusInAndExpiresAtBefore(anyList(), any()))
                    .thenReturn(List.of(p1));
            when(alipayFetcher.fetchAndParse(PaymentChannel.ALIPAY, BILL_DATE))
                    .thenReturn(List.of(new BillEntry(PaymentChannel.ALIPAY,
                            "OUT-1", "CHAN-1", new BigDecimal("99.00"), "TRADE_SUCCESS")));

            int diffs = service.reconcile(PaymentChannel.ALIPAY, BILL_DATE);

            assertThat(diffs).isZero();
            verify(diffRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("渠道有 / 本地缺 → LOCAL_MISSING")
        void shouldDetectLocalMissing() {
            when(paymentRepository.findByStatusInAndExpiresAtBefore(anyList(), any()))
                    .thenReturn(List.of());
            when(alipayFetcher.fetchAndParse(PaymentChannel.ALIPAY, BILL_DATE))
                    .thenReturn(List.of(new BillEntry(PaymentChannel.ALIPAY,
                            "OUT-X", "CHAN-X", new BigDecimal("99.00"), "TRADE_SUCCESS")));

            int diffs = service.reconcile(PaymentChannel.ALIPAY, BILL_DATE);

            assertThat(diffs).isEqualTo(1);
            ArgumentCaptor<List<ReconcileDiff>> captor = ArgumentCaptor.forClass(List.class);
            verify(diffRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getDiffType())
                    .isEqualTo(ReconcileDiffType.LOCAL_MISSING);
        }

        @Test
        @DisplayName("本地有 / 渠道缺 → CHANNEL_MISSING")
        void shouldDetectChannelMissing() {
            LocalDateTime paidAt = LocalDateTime.of(BILL_DATE, java.time.LocalTime.of(10, 0));
            when(paymentRepository.findByStatusInAndExpiresAtBefore(anyList(), any()))
                    .thenReturn(List.of(paid("OUT-1", new BigDecimal("99.00"), paidAt)));
            when(alipayFetcher.fetchAndParse(PaymentChannel.ALIPAY, BILL_DATE))
                    .thenReturn(List.of());

            int diffs = service.reconcile(PaymentChannel.ALIPAY, BILL_DATE);

            assertThat(diffs).isEqualTo(1);
            ArgumentCaptor<List<ReconcileDiff>> captor = ArgumentCaptor.forClass(List.class);
            verify(diffRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getDiffType())
                    .isEqualTo(ReconcileDiffType.CHANNEL_MISSING);
        }

        @Test
        @DisplayName("双方有 / 金额不一致 → AMOUNT_MISMATCH")
        void shouldDetectAmountMismatch() {
            LocalDateTime paidAt = LocalDateTime.of(BILL_DATE, java.time.LocalTime.of(10, 0));
            when(paymentRepository.findByStatusInAndExpiresAtBefore(anyList(), any()))
                    .thenReturn(List.of(paid("OUT-1", new BigDecimal("99.00"), paidAt)));
            when(alipayFetcher.fetchAndParse(PaymentChannel.ALIPAY, BILL_DATE))
                    .thenReturn(List.of(new BillEntry(PaymentChannel.ALIPAY,
                            "OUT-1", "CHAN-1", new BigDecimal("88.00"), "TRADE_SUCCESS")));

            int diffs = service.reconcile(PaymentChannel.ALIPAY, BILL_DATE);

            assertThat(diffs).isEqualTo(1);
            ArgumentCaptor<List<ReconcileDiff>> captor = ArgumentCaptor.forClass(List.class);
            verify(diffRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getDiffType())
                    .isEqualTo(ReconcileDiffType.AMOUNT_MISMATCH);
        }

        @Test
        @DisplayName("无 fetcher 应返回 0 不写表")
        void shouldNoopWhenNoFetcher() {
            // 用一个不支持 ALIPAY 的 fetcher 来覆盖
            when(alipayFetcher.supports(PaymentChannel.ALIPAY)).thenReturn(false);
            ReconcileApplicationService svc = new ReconcileApplicationService(
                    paymentRepository, diffRepository, List.of(alipayFetcher));

            int diffs = svc.reconcile(PaymentChannel.ALIPAY, BILL_DATE);
            assertThat(diffs).isZero();
            verify(diffRepository, never()).saveAll(any());
        }
    }
}
