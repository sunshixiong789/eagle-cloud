package com.eagle.idgenerator.generator;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 Hutool {@link IdUtil#nanoId(int)} 的 NanoId 字符串 ID 生成器。
 *
 * <p>NanoId 特点：
 * <ul>
 *   <li>URL 安全字符集（A-Z、a-z、0-9、_、-）</li>
 *   <li>长度可配置，默认 21 字符（碰撞概率与 UUID v4 相当）</li>
 *   <li>无中心化、无配置依赖，性能优于 UUID v4</li>
 *   <li>常用于短链接、邀请码、对外资源 ID（不暴露顺序信息）</li>
 * </ul>
 *
 * <p>不实现 {@link IdGenerator#nextId()}（NanoId 仅生成字符串，无 long 形式）。
 * 如需 long ID 用 {@link SnowflakeIdGenerator} / {@link TsidIdGenerator}。
 *
 * <p>线程安全：Hutool {@link IdUtil#nanoId(int)} 内部使用 {@code SecureRandom}，线程安全。
 *
 * @author sunshixiong
 */
@Slf4j
public class NanoIdGenerator {

    /**
     * 默认长度 21（与 nanoid.js 默认一致，碰撞概率 ≈ UUID v4）
     */
    public static final int DEFAULT_SIZE = 21;

    private final int defaultSize;

    /**
     * 使用默认长度（21 字符）构造。
     */
    public NanoIdGenerator() {
        this(DEFAULT_SIZE);
    }

    /**
     * 自定义默认长度构造。
     *
     * @param defaultSize 默认 NanoId 长度，需 &gt; 0
     */
    public NanoIdGenerator(int defaultSize) {
        if (defaultSize <= 0) {
            throw new IllegalArgumentException("defaultSize must be > 0, got: " + defaultSize);
        }
        this.defaultSize = defaultSize;
        log.info("NanoIdGenerator initialized: defaultSize={}", defaultSize);
    }

    /**
     * 生成默认长度的 NanoId。
     *
     * @return NanoId 字符串，如 {@code "V1StGXR8_Z5jdHi6B-myT"}
     */
    public String nextId() {
        return IdUtil.nanoId(defaultSize);
    }

    /**
     * 生成指定长度的 NanoId。
     *
     * @param size NanoId 长度（&gt; 0）
     * @return 指定长度的 NanoId 字符串
     */
    public String nextId(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0, got: " + size);
        }
        return IdUtil.nanoId(size);
    }
}
