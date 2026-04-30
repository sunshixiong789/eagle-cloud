package com.eagle.idgenerator.generator;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 基于 UUID v7（time-ordered Unix Epoch，RFC 9562）的 ID 生成器。
 *
 * <p>UUID v7 相比 v4 的优势：
 * <ul>
 *   <li>前 48 位为毫秒级 Unix 时间戳，按字典序天然递增（适合数据库主键 / 索引）</li>
 *   <li>避免 v4 完全随机导致的 B+Tree 页分裂问题（写入性能显著优于 v4）</li>
 *   <li>仍保留足够随机位（74 bit）保证全局唯一性</li>
 * </ul>
 *
 * <p>提供两种 ID 形态：
 * <ul>
 *   <li>{@link #nextId()} — UUID 高 64 位转为 long（含时间戳前缀，趋势递增）</li>
 *   <li>{@link #nextIdStr()} — 32 位十六进制字符串（去掉连字符）</li>
 *   <li>{@link #nextUuid()} — 原始 UUID 对象</li>
 * </ul>
 *
 * <p>实现委托 {@code com.github.f4b6a3:uuid-creator} 的 {@link UuidCreator#getTimeOrderedEpoch()}，
 * 该库由 RFC 9562 草案作者之一维护，覆盖 v1/v3/v4/v5/v6/v7。
 *
 * <p>线程安全：{@link UuidCreator#getTimeOrderedEpoch()} 内部使用线程安全工厂，本类无可变状态。
 *
 * @author sunshixiong
 */
@Slf4j
public class UuidIdGenerator implements IdGenerator {

    /**
     * 返回 UUID v7 的高 64 位作为 long 型 ID。
     *
     * <p>高 64 位包含 48 bit 时间戳 + 4 bit 版本号 + 12 bit 随机数，
     * 整体趋势递增，适合作为数据库主键（避免随机插入导致的索引碎片）。
     *
     * @return UUID v7 的高 64 位 long 值
     */
    @Override
    public long nextId() {
        return UuidCreator.getTimeOrderedEpoch().getMostSignificantBits();
    }

    /**
     * 生成去掉连字符的 32 位十六进制 UUID v7 字符串。
     *
     * <p>示例：{@code "018f3a1b2c9d7e4f5g6h7i8j9k0l1m2n"}（32 位、无连字符、全小写）。
     *
     * @return 32 位 UUID v7 字符串
     */
    @Override
    public String nextIdStr() {
        return UuidCreator.getTimeOrderedEpoch().toString().replace("-", "");
    }

    /**
     * 生成原始 UUID v7 对象（含连字符的 36 位标准格式）。
     *
     * @return UUID v7 对象，{@link UUID#toString()} 形如
     *         {@code "018f3a1b-2c9d-7e4f-9g6h-7i8j9k0l1m2n"}
     */
    public UUID nextUuid() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
