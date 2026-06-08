package com.eagle.payment.core.domain.port;

import com.eagle.payment.core.domain.model.enums.PaymentChannel;

import java.time.LocalDate;
import java.util.List;

/**
 * 渠道清算单拉取 + 解析端口 (Driven Port)。
 *
 * <p>对账流程: ReconcileApplicationService 调用本接口拉取指定日期的渠道清算单,
 * 解析为统一的 {@link BillEntry} 列表后,与本地 Payment 表三元组 (outTradeNo, amount,
 * status) 比对,差异落 {@code t_reconcile_diff}。
 *
 * <p>实现注意:
 * <ul>
 *   <li>支付宝 - {@code alipay.data.dataservice.bill.downloadurl.query} 获取下载 URL → 拉取 zip → 解 CSV</li>
 *   <li>微信   - {@code GET /v3/bill/tradebill?bill_date=...&bill_type=ALL} → download_url → 解密 → 解 CSV</li>
 * </ul>
 *
 * @author sunshixiong
 */
public interface ReconcileBillFetchPort {

    /**
     * 拉取并解析指定日期 + 渠道的清算单。
     *
     * @param channel 渠道
     * @param billDate 清算单对应业务日期 (T-1 通常)
     * @return 已解析的清算条目;无数据返回空列表;无法拉取抛 ServiceException
     */
    List<BillEntry> fetchAndParse(PaymentChannel channel, LocalDate billDate);

    /**
     * 渠道侧本日是否支持的本地适配器。
     */
    boolean supports(PaymentChannel channel);
}
