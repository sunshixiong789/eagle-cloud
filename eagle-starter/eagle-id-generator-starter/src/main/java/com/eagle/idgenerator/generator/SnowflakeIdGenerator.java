package com.eagle.idgenerator.generator;

import com.eagle.idgenerator.properties.IdGeneratorProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 雪花算法 ID 生成器。
 *
 * <p>ID 结构（64 bit）：
 * <pre>
 * | 符号位(1) | 时间戳差值(41) | 数据中心ID(5) | 机器ID(5) | 序列号(12) |
 * </pre>
 *
 * <ul>
 *   <li>时间戳精度：毫秒，起始时间默认 2024-01-01T00:00:00Z，可用约 69 年</li>
 *   <li>每毫秒最多生成 4096 个 ID（序列号 12 bit），耗尽时自旋等待到下一毫秒</li>
 *   <li>支持 32 个数据中心 × 32 台机器 = 1024 个节点</li>
 *   <li>时钟回拨 {@value #MAX_BACKWARD_MS}ms 内自旋等待追平，超出则抛 {@link IllegalStateException}</li>
 * </ul>
 *
 * <p>为 native image 友好，本类不依赖任何第三方库（原实现委托 Hutool，已移除）。
 *
 * <p>线程安全：临界区由 {@link ReentrantLock} 保护。不用 {@code synchronized} 是因为
 * 序列耗尽时会自旋等待，虚拟线程下 {@code ReentrantLock} 语义更明确（见 {@code 01-java25.md}）。
 *
 * @author sunshixiong
 */
@Slf4j
public class SnowflakeIdGenerator implements IdGenerator {

    /**
     * 默认起始时间戳：2024-01-01T00:00:00Z 的毫秒值
     */
    private static final long DEFAULT_EPOCH = 1704067200000L;

    /**
     * 时钟回拨容忍上限（毫秒），超出直接抛异常而非静默生成重复 ID
     */
    private static final long MAX_BACKWARD_MS = 2000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;
    private final ReentrantLock lock = new ReentrantLock();

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 构造雪花算法 ID 生成器。
     *
     * @param properties ID 生成器配置属性
     * @throws IllegalArgumentException workerId 或 datacenterId 超出 [0, 31] 时抛出
     */
    public SnowflakeIdGenerator(IdGeneratorProperties properties) {
        long wId = properties.getWorkerId();
        long dId = properties.getDatacenterId();
        if (wId < 0 || wId > MAX_WORKER_ID) {
            throw new IllegalArgumentException(
                    "workerId must be in [0, " + MAX_WORKER_ID + "], got: " + wId);
        }
        if (dId < 0 || dId > MAX_DATACENTER_ID) {
            throw new IllegalArgumentException(
                    "datacenterId must be in [0, " + MAX_DATACENTER_ID + "], got: " + dId);
        }
        this.workerId = wId;
        this.datacenterId = dId;
        log.info("SnowflakeIdGenerator initialized: datacenterId={}, workerId={}", dId, wId);
    }

    @Override
    public long nextId() {
        lock.lock();
        try {
            long timestamp = System.currentTimeMillis();

            if (timestamp < lastTimestamp) {
                long offset = lastTimestamp - timestamp;
                if (offset > MAX_BACKWARD_MS) {
                    throw new IllegalStateException(
                            "Clock moved backwards by " + offset + "ms (> " + MAX_BACKWARD_MS
                                    + "ms), refusing to generate id");
                }
                // 小幅回拨：等到追平上次时间戳，随后按同毫秒序列递增处理
                timestamp = waitUntil(lastTimestamp);
            }

            if (timestamp == lastTimestamp) {
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    // 当前毫秒序列耗尽，等到下一毫秒
                    timestamp = waitUntil(lastTimestamp + 1);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            return ((timestamp - DEFAULT_EPOCH) << TIMESTAMP_SHIFT)
                    | (datacenterId << DATACENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String nextIdStr() {
        return Long.toString(nextId());
    }

    /**
     * 自旋等待直到系统时钟到达目标毫秒。
     *
     * @param target 目标时间戳（毫秒）
     * @return 到达后的当前时间戳，保证 &gt;= target
     */
    private static long waitUntil(long target) {
        long timestamp = System.currentTimeMillis();
        while (timestamp < target) {
            Thread.onSpinWait();
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
