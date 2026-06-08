package com.eagle.payment.core.infrastructure.schedule;

import com.eagle.payment.core.application.service.ReconcileApplicationService;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * XXL-JOB 对账任务。
 *
 * <p>调度中心配置:
 * <ul>
 *   <li>任务名: {@code paymentReconcileJob}</li>
 *   <li>cron 推荐: {@code 0 0 2 * * ?} (每日凌晨 2:00 跑前一天)</li>
 *   <li>参数 (JobParam,JSON): {@code {"billDate":"2026-06-08"}}; 留空则默认 T-1</li>
 *   <li>路由策略: 第一个 (避免多副本并发)</li>
 *   <li>阻塞策略: 单机串行</li>
 *   <li>失败重试: 0 (对账幂等性已通过 diff 表唯一性保证,但人工触发更稳)</li>
 * </ul>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eagle.payment.reconcile", name = "enabled", havingValue = "true")
public class ReconcileJob {

    private final ReconcileApplicationService reconcileApplicationService;

    @XxlJob("paymentReconcileJob")
    public void execute() {
        LocalDate billDate = LocalDate.now().minusDays(1);
        log.info("paymentReconcileJob start, billDate={}", billDate);
        int total = 0;
        for (PaymentChannel channel : PaymentChannel.values()) {
            try {
                total += reconcileApplicationService.reconcile(channel, billDate);
            } catch (RuntimeException ex) {
                log.error("reconcile failed, channel={}, billDate={}",
                        channel, billDate, ex);
            }
        }
        log.info("paymentReconcileJob done, totalDiffs={}, billDate={}", total, billDate);
    }
}
