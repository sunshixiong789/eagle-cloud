package com.eagle.idgenerator.generator;

import cn.hutool.core.lang.Snowflake;
import com.eagle.idgenerator.properties.IdGeneratorProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * 基于 Hutool {@link Snowflake} 的雪花算法 ID 生成器。
 *
 * <p>ID 结构（64 bit）：
 * <pre>
 * | 符号位(1) | 时间戳差值(41) | 数据中心ID(5) | 机器ID(5) | 序列号(12) |
 * </pre>
 *
 * <ul>
 *   <li>时间戳精度：毫秒，起始时间默认 2024-01-01T00:00:00Z</li>
 *   <li>每毫秒最多生成 4096 个 ID（序列号 12 bit）</li>
 *   <li>支持 32 个数据中心 × 32 台机器 = 1024 个节点</li>
 *   <li>Hutool 内置时钟回拨容忍（默认 2000ms 内自旋等待，超出抛异常）</li>
 * </ul>
 *
 * <p>实现委托 Hutool {@code cn.hutool.core.lang.Snowflake}，本类仅做参数校验和日志包装。
 *
 * @author sunshixiong
 */
@Slf4j
public class SnowflakeIdGenerator implements IdGenerator {

    /** 默认起始时间戳：2024-01-01T00:00:00Z 的毫秒值 */
    private static final long DEFAULT_EPOCH = 1704067200000L;

    private final Snowflake snowflake;

    /**
     * 构造雪花算法 ID 生成器。
     *
     * @param properties ID 生成器配置属性
     * @throws IllegalArgumentException workerId 或 datacenterId 超出范围时抛出
     */
    public SnowflakeIdGenerator(IdGeneratorProperties properties) {
        long wId = properties.getWorkerId();
        long dId = properties.getDatacenterId();
        this.snowflake = new Snowflake(new Date(DEFAULT_EPOCH), wId, dId, false);
        log.info("SnowflakeIdGenerator (Hutool) initialized: datacenterId={}, workerId={}", dId, wId);
    }

    @Override
    public long nextId() {
        return snowflake.nextId();
    }

    @Override
    public String nextIdStr() {
        return snowflake.nextIdStr();
    }
}
