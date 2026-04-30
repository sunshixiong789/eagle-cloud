package com.eagle.idgenerator.util;

import com.eagle.idgenerator.generator.IdGenerator;
import com.eagle.idgenerator.generator.NanoIdGenerator;
import com.eagle.idgenerator.generator.OrderNoGenerator;
import com.eagle.idgenerator.generator.TsidIdGenerator;
import com.eagle.idgenerator.generator.UuidIdGenerator;
import com.github.f4b6a3.tsid.Tsid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * ID 生成器统一门面。
 *
 * <p>聚合 Snowflake / UUID v7 / TSID / NanoId / 业务订单号 五种生成能力，
 * 业务代码通过本门面获取所需 ID，无需关心底层实现切换。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class OrderApplicationService {
 *
 *     private final IdGeneratorFacade idFacade;
 *
 *     public void createOrder(...) {
 *         long pk = idFacade.nextId();              // 默认实现（按配置）
 *         long snowflake = idFacade.snowflakeId();  // 雪花算法
 *         UUID uuid7 = idFacade.uuidV7();           // UUID v7
 *         String tsid = idFacade.tsidStr();         // TSID 13 位字符串
 *         String inviteCode = idFacade.nanoId(8);   // 短邀请码
 *         String orderNo = idFacade.orderNo("ORD"); // 业务单号
 *     }
 * }
 * }</pre>
 *
 * @author sunshixiong
 */
@Slf4j
@RequiredArgsConstructor
public class IdGeneratorFacade {

    private static final String PAY_PREFIX = "PAY";
    private static final String REFUND_PREFIX = "RFD";

    private final IdGenerator defaultGenerator;
    private final UuidIdGenerator uuidGenerator;
    private final TsidIdGenerator tsidGenerator;
    private final NanoIdGenerator nanoIdGenerator;
    private final OrderNoGenerator orderNoGenerator;

    // ==================== 默认 IdGenerator（按 type 配置切换）====================

    /**
     * 默认 {@link IdGenerator} 生成 long ID。
     */
    public long nextId() {
        return defaultGenerator.nextId();
    }

    /**
     * 默认 {@link IdGenerator} 生成 String ID。
     */
    public String nextIdStr() {
        return defaultGenerator.nextIdStr();
    }

    // ==================== Snowflake ====================

    /**
     * 雪花算法 long ID（即默认 {@code IdGenerator.nextId()}，保留语义化方法）。
     */
    public long snowflakeId() {
        return defaultGenerator.nextId();
    }

    // ==================== UUID v7 ====================

    /**
     * UUID v7 高 64 位 long ID（趋势递增）。
     */
    public long uuidLong() {
        return uuidGenerator.nextId();
    }

    /**
     * 32 位 UUID v7 字符串（去连字符）。
     */
    public String uuid() {
        return uuidGenerator.nextIdStr();
    }

    /**
     * 原始 UUID v7 对象（含连字符的 36 位标准格式）。
     */
    public UUID uuidV7() {
        return uuidGenerator.nextUuid();
    }

    // ==================== TSID ====================

    /**
     * TSID long 形式。
     */
    public long tsidLong() {
        return tsidGenerator.nextId();
    }

    /**
     * TSID 13 位 Crockford Base32 字符串。
     */
    public String tsidStr() {
        return tsidGenerator.nextIdStr();
    }

    /**
     * 原始 {@link Tsid} 对象。
     */
    public Tsid tsid() {
        return tsidGenerator.nextTsid();
    }

    // ==================== NanoId ====================

    /**
     * 默认长度 NanoId（21 字符）。
     */
    public String nanoId() {
        return nanoIdGenerator.nextId();
    }

    /**
     * 指定长度 NanoId。
     */
    public String nanoId(int size) {
        return nanoIdGenerator.nextId(size);
    }

    // ==================== 业务订单号 ====================

    /**
     * 带业务前缀的订单号，如 {@code "ORD20260430123456789"}。
     */
    public String orderNo(String prefix) {
        return orderNoGenerator.generate(prefix);
    }

    /**
     * 无前缀订单号，如 {@code "20260430123456789"}。
     */
    public String orderNo() {
        return orderNoGenerator.generate();
    }

    /**
     * 支付流水号（前缀 {@code PAY}）。
     */
    public String payNo() {
        return orderNoGenerator.generate(PAY_PREFIX);
    }

    /**
     * 退款流水号（前缀 {@code RFD}）。
     */
    public String refundNo() {
        return orderNoGenerator.generate(REFUND_PREFIX);
    }
}
