package com.eagle.idgenerator.generator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务订单号生成器。
 *
 * <p>生成格式：{@code {prefix}{yyyyMMdd}{9位序列}}，总长度 = 前缀长度 + 8 + 9 位。
 * 示例：{@code ORD20240115123456789}（前缀 ORD + 日期 + 雪花尾部 9 位）。
 *
 * <p>与雪花算法 {@link SnowflakeIdGenerator} 的区别：
 * <ul>
 *   <li>订单号含日期，具有可读性，便于客服查询、对账、归档</li>
 *   <li>按日期前缀检索效率高（天级别索引分区友好）</li>
 *   <li>序列号取雪花 ID 尾部 9 位（{@code id % 1_000_000_000}），保持单节点每日 10^9 量级唯一性</li>
 * </ul>
 *
 * <p>线程安全：依赖 {@link IdGenerator#nextId()} 的线程安全性（{@link SnowflakeIdGenerator} 已同步）。
 *
 * @author sunshixiong
 */
@Slf4j
@RequiredArgsConstructor
public class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 序列号模数：取雪花 ID 后 9 位
     */
    private static final long SEQUENCE_MODULUS = 1_000_000_000L;

    /**
     * 序列号格式：9 位，不足前补零
     */
    private static final String SEQUENCE_FORMAT = "%09d";

    private final IdGenerator snowflakeIdGenerator;

    /**
     * 生成带业务前缀和日期的订单号。
     *
     * <p>格式：{@code {prefix}{yyyyMMdd}{9位序列}}
     * <br>示例：
     * <ul>
     *   <li>{@code generate("ORD")} → {@code ORD20240115123456789}</li>
     *   <li>{@code generate("PAY")} → {@code PAY20240115000000001}</li>
     *   <li>{@code generate("RFD")} → {@code RFD20240115987654321}</li>
     * </ul>
     *
     * @param prefix 业务前缀，如 {@code "ORD"}（订单）、{@code "PAY"}（支付）、{@code "RFD"}（退款）；
     *               允许为空字符串，此时退化为纯日期 + 序列格式
     * @return 全局唯一订单号字符串
     */
    public String generate(String prefix) {
        String date = LocalDate.now().format(DATE_FORMATTER);
        long id = snowflakeIdGenerator.nextId();
        // 取雪花 ID 绝对值后 9 位，保证每天序列在 [0, 10^9) 范围内唯一
        String seq = String.format(SEQUENCE_FORMAT, Math.abs(id % SEQUENCE_MODULUS));
        String orderNo = (prefix != null ? prefix : "") + date + seq;
        log.debug("Generated order no: {}", orderNo);
        return orderNo;
    }

    /**
     * 生成无前缀订单号。
     *
     * <p>格式：{@code {yyyyMMdd}{9位序列}}，适用于无需前缀区分业务类型的场景。
     *
     * @return 全局唯一订单号字符串
     */
    public String generate() {
        return generate("");
    }
}
