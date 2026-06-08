package com.eagle.payment.core.application.service;

import com.eagle.payment.core.domain.model.aggregate.Payment;
import com.eagle.payment.core.domain.model.aggregate.ReconcileDiff;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.model.enums.PaymentStatus;
import com.eagle.payment.core.domain.port.BillEntry;
import com.eagle.payment.core.domain.port.ReconcileBillFetchPort;
import com.eagle.payment.core.domain.repository.PaymentRepository;
import com.eagle.payment.core.domain.repository.ReconcileDiffRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对账应用服务: 拉清算单 → 三元组比对本地 Payment → 落差异表 + 告警。
 *
 * <p>对账维度: 按 {@code (channel, billDate)} 一次任务完成。每条本地 PAID 的 Payment
 * 要在渠道清算单上有对应一条;每条清算单要在本地有对应 PAID Payment;金额必须一致。
 * 任何不匹配都落 {@code t_reconcile_diff} 表。
 *
 * <p>跨服务事件: 发现差异时发布 {@code payment_reconcile_events} {@code diff-detected}
 * 事件 (本 ApplicationService 内仅落表,事件由桥接处理器统一发布)。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
public class ReconcileApplicationService {

    private final PaymentRepository paymentRepository;
    private final ReconcileDiffRepository diffRepository;
    private final List<ReconcileBillFetchPort> fetchers;

    public ReconcileApplicationService(PaymentRepository paymentRepository,
                                       ReconcileDiffRepository diffRepository,
                                       List<ReconcileBillFetchPort> fetchers) {
        this.paymentRepository = paymentRepository;
        this.diffRepository = diffRepository;
        this.fetchers = fetchers;
    }

    /**
     * 执行 (channel, billDate) 维度的对账。
     *
     * @return 本次发现的差异数量
     */
    @Transactional
    public int reconcile(PaymentChannel channel, LocalDate billDate) {
        ReconcileBillFetchPort fetcher = fetchers.stream()
                .filter(f -> f.supports(channel))
                .findFirst()
                .orElse(null);
        if (fetcher == null) {
            log.warn("no reconcile fetcher for channel={}", channel);
            return 0;
        }
        List<BillEntry> entries = fetcher.fetchAndParse(channel, billDate);
        log.info("reconcile fetched {} entries, channel={}, billDate={}",
                entries.size(), channel, billDate);

        // 本地当日 PAID 订单
        LocalDateTime dayStart = LocalDateTime.of(billDate, LocalTime.MIN);
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<Payment> localPaid = paymentRepository.findByStatusInAndExpiresAtBefore(
                List.of(PaymentStatus.PAID), dayEnd);
        Map<String, Payment> localByOutTradeNo = new HashMap<>();
        for (Payment p : localPaid) {
            if (p.getChannel() == channel && p.getPaidAt() != null
                    && !p.getPaidAt().isBefore(dayStart) && p.getPaidAt().isBefore(dayEnd)
                    && p.getOutTradeNo() != null) {
                localByOutTradeNo.put(p.getOutTradeNo(), p);
            }
        }

        List<ReconcileDiff> diffs = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (BillEntry entry : entries) {
            visited.add(entry.outTradeNo());
            Payment local = localByOutTradeNo.get(entry.outTradeNo());
            if (local == null) {
                diffs.add(ReconcileDiff.localMissing(billDate, channel, entry.outTradeNo(),
                        entry.amount(), entry.channelStatus()));
                continue;
            }
            if (local.getAmount().compareTo(entry.amount()) != 0) {
                diffs.add(ReconcileDiff.amountMismatch(billDate, channel, entry.outTradeNo(),
                        local.getId(), local.getAmount(), entry.amount()));
            }
            // 状态校验: 渠道清单只列结算成功的;本地必为 PAID,否则 STATUS_MISMATCH
            if (local.getStatus() != PaymentStatus.PAID) {
                diffs.add(ReconcileDiff.statusMismatch(billDate, channel, entry.outTradeNo(),
                        local.getId(), local.getStatus().name(), entry.channelStatus()));
            }
        }
        // 本地有但渠道清单中缺失
        for (Map.Entry<String, Payment> kv : localByOutTradeNo.entrySet()) {
            if (!visited.contains(kv.getKey())) {
                Payment local = kv.getValue();
                diffs.add(ReconcileDiff.channelMissing(billDate, channel, kv.getKey(),
                        local.getId(), local.getAmount(), local.getStatus().name()));
            }
        }

        if (!diffs.isEmpty()) {
            diffRepository.saveAll(diffs);
            log.warn("reconcile detected {} diffs, channel={}, billDate={}",
                    diffs.size(), channel, billDate);
        } else {
            log.info("reconcile clean, channel={}, billDate={}", channel, billDate);
        }
        return diffs.size();
    }
}
