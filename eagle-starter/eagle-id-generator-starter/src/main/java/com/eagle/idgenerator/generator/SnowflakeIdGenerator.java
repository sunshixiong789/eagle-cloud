package com.eagle.idgenerator.generator;

import com.eagle.idgenerator.properties.IdGeneratorProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于雪花算法（Snowflake）的分布式唯一 ID 生成器。
 *
 * <p>ID 结构（64 bit）：
 * <pre>
 * | 符号位(1) | 时间戳差值(41) | 数据中心ID(5) | 机器ID(5) | 序列号(12) |
 * </pre>
 *
 * <ul>
 *   <li>时间戳精度：毫秒，起始时间 2024-01-01T00:00:00Z</li>
 *   <li>支持最长 69 年（2024 年起计）</li>
 *   <li>每毫秒最多生成 4096 个 ID（序列号 12 bit）</li>
 *   <li>支持 32 个数据中心 × 32 台机器 = 1024 个节点</li>
 * </ul>
 *
 * <p>时钟回拨检测：检测到时钟回拨时直接抛出异常，不静默重试，由调用方决定降级策略。
 *
 * @author sunshixiong
 */
@Slf4j
public class SnowflakeIdGenerator implements IdGenerator {

    // ==================== 位分配常量 ====================

    /** 序列号占用的位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 机器 ID 占用的位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心 ID 占用的位数 */
    private static final long DATACENTER_ID_BITS = 5L;

    /** 机器 ID 最大值 31 */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 数据中心 ID 最大值 31 */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /** 序列号掩码 4095 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 机器 ID 左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心 ID 左移位数 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 起始时间戳：2024-01-01T00:00:00Z 的毫秒值。
     * 固定值，禁止修改——修改后历史 ID 将无法正确解析时间戳。
     */
    private static final long EPOCH = 1704067200000L;

    // ==================== 实例字段 ====================

    /** 数据中心 ID */
    private final long datacenterId;

    /** 工作机器 ID */
    private final long workerId;

    /** 毫秒内序列号，范围 [0, 4095] */
    private long sequence;

    /** 上次生成 ID 的时间戳（毫秒） */
    private long lastTimestamp = -1L;

    /**
     * 构造雪花算法 ID 生成器。
     *
     * @param properties ID 生成器配置属性
     * @throws IllegalArgumentException workerId 或 datacenterId 超出范围时抛出
     */
    public SnowflakeIdGenerator(IdGeneratorProperties properties) {
        long wId = properties.getWorkerId();
        long dId = properties.getDatacenterId();

        if (wId > MAX_WORKER_ID || wId < 0) {
            throw new IllegalArgumentException(
                    String.format("workerId must be in [0, %d], got: %d", MAX_WORKER_ID, wId));
        }
        if (dId > MAX_DATACENTER_ID || dId < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenterId must be in [0, %d], got: %d", MAX_DATACENTER_ID, dId));
        }

        this.workerId = wId;
        this.datacenterId = dId;
        this.sequence = properties.getSequence();

        log.info("SnowflakeIdGenerator initialized: datacenterId={}, workerId={}", dId, wId);
    }

    /**
     * 生成下一个唯一 ID（线程安全）。
     *
     * @return 全局唯一的 long 型 ID
     * @throws IllegalStateException 发生时钟回拨时抛出（时钟回拨不静默重试）
     */
    @Override
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 检测时钟回拨：当前时间小于上次记录时间
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            log.error("Clock moved backwards! Refusing to generate ID for {}ms, workerId={}, datacenterId={}",
                    offset, workerId, datacenterId);
            throw new IllegalStateException(
                    String.format("Clock moved backwards. Refusing for %d milliseconds", offset));
        }

        if (lastTimestamp == timestamp) {
            // 同一毫秒内，序列号自增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的毫秒，序列号归零
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成下一个唯一 ID 的字符串形式。
     *
     * @return 全局唯一 ID 的字符串表示
     */
    @Override
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 自旋等待直到获取下一毫秒时间戳。
     *
     * @param lastTimestamp 上次生成时的时间戳
     * @return 严格大于 lastTimestamp 的当前时间戳
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳（毫秒），封装为方法便于单元测试覆写。
     *
     * @return 当前 Unix 时间戳（毫秒）
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
