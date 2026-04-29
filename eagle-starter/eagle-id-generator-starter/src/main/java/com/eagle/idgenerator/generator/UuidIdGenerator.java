package com.eagle.idgenerator.generator;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 基于 {@link UUID#randomUUID()} 的 ID 生成器。
 *
 * <p>提供两种 ID 形态：
 * <ul>
 *   <li>{@link #nextId()} — 将 UUID 高 64 位转为 long（符号位不置零，可能为负数）</li>
 *   <li>{@link #nextIdStr()} — 去掉连字符的 32 位十六进制字符串（无序，全局唯一）</li>
 * </ul>
 *
 * <p>适用场景：
 * <ul>
 *   <li>不依赖中心化节点，无需配置 workerId / datacenterId</li>
 *   <li>对 ID 可读性要求不高，接受无序字符串</li>
 *   <li>低并发场景，不需要雪花算法的高吞吐量</li>
 * </ul>
 *
 * <p>注意：{@link #nextId()} 的 long 值唯一性概率理论上非 100%（UUID 碰撞概率极低），
 * 如需强唯一性保证，优先使用 {@link SnowflakeIdGenerator}。
 *
 * <p>线程安全：{@link UUID#randomUUID()} 本身线程安全，本类无共享可变状态。
 *
 * @author sunshixiong
 */
@Slf4j
public class UuidIdGenerator implements IdGenerator {

    /**
     * 将 UUID 高 64 位作为 long 型 ID 返回。
     *
     * <p>同一 UUID 的高低 64 位相互独立，高 64 位已具备足够的随机性。
     * 返回值可能为负数（符号位由随机决定），使用方须注意。
     *
     * @return UUID 的高 64 位 long 值
     */
    @Override
    public long nextId() {
        return UUID.randomUUID().getMostSignificantBits();
    }

    /**
     * 生成去掉连字符的 32 位十六进制 UUID 字符串。
     *
     * <p>示例：{@code "550e8400e29b41d4a716446655440000"}（32 位，无连字符，全小写）。
     *
     * @return 32 位 UUID 字符串
     */
    @Override
    public String nextIdStr() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
