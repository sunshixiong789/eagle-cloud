package com.eagle.idgenerator.generator;

/**
 * 分布式唯一 ID 生成器接口。
 *
 * <p>提供 long 型和 String 型两种 ID 生成方式，具体实现由各策略类提供
 * （默认实现为 {@link SnowflakeIdGenerator}）。
 *
 * @author sunshixiong
 */
public interface IdGenerator {

    /**
     * 生成下一个 long 型唯一 ID。
     *
     * @return 全局唯一的 long 型 ID
     */
    long nextId();

    /**
     * 生成下一个 String 型唯一 ID。
     *
     * @return 全局唯一的字符串 ID（{@link #nextId()} 的字符串形式）
     */
    String nextIdStr();
}
