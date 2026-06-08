package com.eagle.payment.core.infrastructure.reconcile;

import com.eagle.payment.core.common.exception.ReconcileErrorCode;
import com.eagle.payment.core.domain.model.enums.PaymentChannel;
import com.eagle.payment.core.domain.port.BillEntry;
import com.eagle.payment.core.domain.port.ReconcileBillFetchPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 支付宝清算单拉取适配器 (骨架)。
 *
 * <p>v1 留位置 / 抛 ServiceException 标识"暂未实现",待对账上线前补全:
 * <ol>
 *   <li>用 {@code AlipayDataDataserviceBillDownloadurlQueryRequest} 取下载 URL</li>
 *   <li>HTTP GET 拉 zip 文件 → 解压 → 取 CSV</li>
 *   <li>跳过表头,按列映射为 BillEntry (out_trade_no / trade_no / total_amount /
 *       trade_status)</li>
 * </ol>
 *
 * <p>对账核心逻辑 ({@code ReconcileApplicationService}) 已可工作,接 mock 实现即可端到端跑通;
 * P1-2 后期补完此适配器即可上线。
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "eagle.payment.reconcile", name = "enabled", havingValue = "true")
public class AlipayReconcileBillFetcher implements ReconcileBillFetchPort {

    @Override
    public List<BillEntry> fetchAndParse(PaymentChannel channel, LocalDate billDate) {
        log.warn("AlipayReconcileBillFetcher 骨架未实现 - billDate={}", billDate);
        throw ReconcileErrorCode.FETCH_FAILED.toServiceException();
    }

    @Override
    public boolean supports(PaymentChannel channel) {
        return channel == PaymentChannel.ALIPAY;
    }
}
