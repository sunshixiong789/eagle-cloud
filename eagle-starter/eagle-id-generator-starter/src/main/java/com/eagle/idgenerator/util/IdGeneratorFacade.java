package com.eagle.idgenerator.util;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.idgenerator.generator.OrderNoGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ID 生成器统一门面。
 *
 * <p>聚合雪花算法 ID 生成器（{@link IdGenerator}）和订单号生成器（{@link OrderNoGenerator}），
 * 对外提供语义化的业务流水号生成方法，避免业务代码直接依赖具体实现类。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class OrderApplicationService {
 *
 *     private final IdGeneratorFacade idGeneratorFacade;
 *
 *     public void createOrder(CreateOrderRequest request) {
 *         long id = idGeneratorFacade.snowflakeId();      // 数据库主键
 *         String orderNo = idGeneratorFacade.orderNo("ORD"); // 对外业务单号
 *         String payNo = idGeneratorFacade.payNo();          // 支付流水号
 *     }
 * }
 * }</pre>
 *
 * @author sunshixiong
 */
@Slf4j
@RequiredArgsConstructor
public class IdGeneratorFacade {

    /** 支付流水号前缀 */
    private static final String PAY_PREFIX = "PAY";

    /** 退款流水号前缀 */
    private static final String REFUND_PREFIX = "RFD";

    private final IdGenerator snowflakeGenerator;
    private final OrderNoGenerator orderNoGenerator;

    /**
     * 生成雪花算法 long 型唯一 ID。
     *
     * <p>适用于数据库主键、内部系统 ID 等场景。
     *
     * @return 全局唯一的 long 型 ID
     */
    public long snowflakeId() {
        return snowflakeGenerator.nextId();
    }

    /**
     * 生成带业务前缀的订单号。
     *
     * <p>格式：{@code {prefix}{yyyyMMdd}{9位序列}}，具有可读性，便于客服查询和对账。
     *
     * @param prefix 业务前缀，如 {@code "ORD"}（订单）、{@code "SHP"}（发货单）等
     * @return 唯一订单号，如 {@code "ORD20240115123456789"}
     */
    public String orderNo(String prefix) {
        return orderNoGenerator.generate(prefix);
    }

    /**
     * 生成无前缀订单号。
     *
     * <p>格式：{@code {yyyyMMdd}{9位序列}}，适用于无需前缀区分类型的场景。
     *
     * @return 唯一订单号，如 {@code "20240115123456789"}
     */
    public String orderNo() {
        return orderNoGenerator.generate();
    }

    /**
     * 生成支付流水号（前缀 {@code PAY}）。
     *
     * <p>格式：{@code PAY{yyyyMMdd}{9位序列}}，用于唯一标识一笔支付请求。
     *
     * @return 唯一支付流水号，如 {@code "PAY20240115123456789"}
     */
    public String payNo() {
        return orderNoGenerator.generate(PAY_PREFIX);
    }

    /**
     * 生成退款流水号（前缀 {@code RFD}）。
     *
     * <p>格式：{@code RFD{yyyyMMdd}{9位序列}}，用于唯一标识一笔退款请求。
     *
     * @return 唯一退款流水号，如 {@code "RFD20240115987654321"}
     */
    public String refundNo() {
        return orderNoGenerator.generate(REFUND_PREFIX);
    }
}
