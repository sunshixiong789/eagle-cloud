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
 * 微信清算单拉取适配器 (骨架)。
 *
 * <p>v1 留位置,待补全:
 * <ol>
 *   <li>GET /v3/bill/tradebill?bill_date=YYYY-MM-DD&amp;bill_type=ALL 取下载 URL</li>
 *   <li>下载 gzip CSV 文件 (V3 加密,需用 AesGcm 解密)</li>
 *   <li>跳过表头,按列映射为 BillEntry</li>
 * </ol>
 *
 * @author sunshixiong
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "eagle.payment.reconcile", name = "enabled", havingValue = "true")
public class WechatReconcileBillFetcher implements ReconcileBillFetchPort {

    @Override
    public List<BillEntry> fetchAndParse(PaymentChannel channel, LocalDate billDate) {
        log.warn("WechatReconcileBillFetcher 骨架未实现 - billDate={}", billDate);
        throw ReconcileErrorCode.FETCH_FAILED.toServiceException();
    }

    @Override
    public boolean supports(PaymentChannel channel) {
        return channel == PaymentChannel.WECHAT;
    }
}
