package com.eagle.idgenerator.generator;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 号段模式 ID 生成器（美团 Leaf 思路简化版）。
 *
 * <p>原理：从数据库一次性申请一批连续 ID（号段），在内存中用 {@link AtomicLong} 顺序分配，
 * 号段耗尽后再次访问数据库申请下一批。减少了数据库访问频率，兼顾高可用和趋势递增。
 *
 * <p>特点：
 * <ul>
 *   <li>ID 趋势递增（同一号段内严格递增），适合数据库主键</li>
 *   <li>不依赖系统时钟，不受时钟回拨影响</li>
 *   <li>可读性强（纯数字递增），便于人工核查</li>
 *   <li>号段步长（step）可通过数据库动态调整</li>
 * </ul>
 *
 * <p>数据库表结构（需使用方预先创建）：
 * <pre>{@code
 * CREATE TABLE t_id_segment (
 *     biz_tag     VARCHAR(64)  NOT NULL COMMENT '业务标签，每个业务使用独立行',
 *     max_id      BIGINT       NOT NULL DEFAULT 0 COMMENT '当前已分配的最大 ID',
 *     step        INT          NOT NULL DEFAULT 1000 COMMENT '每次申请的号段步长',
 *     description VARCHAR(256) COMMENT '业务说明',
 *     PRIMARY KEY (biz_tag)
 * ) COMMENT = 'ID 号段表';
 * }</pre>
 *
 * <p>线程安全：{@link #nextId()} 使用 {@link AtomicLong} 无锁分配；
 * {@link #loadSegment()} 使用 {@code synchronized} 确保仅一个线程从数据库申请号段。
 *
 * @author sunshixiong
 */
@Slf4j
public class SegmentIdGenerator implements IdGenerator {

    private static final String UPDATE_SQL =
            "UPDATE t_id_segment SET max_id = max_id + step WHERE biz_tag = ?";

    private static final String SELECT_SQL =
            "SELECT max_id, step FROM t_id_segment WHERE biz_tag = ?";

    /**
     * 数据源，用于 JDBC 操作
     */
    private final DataSource dataSource;

    /**
     * 业务标签，对应 t_id_segment 中的 biz_tag 列，用于多业务隔离
     */
    private final String bizTag;

    /**
     * 当前号段已分配到的位置（原子自增，无需加锁）
     */
    private final AtomicLong current = new AtomicLong(0L);

    /**
     * 当前号段的上限（exclusive），超过此值需申请新号段
     */
    private volatile long maxId = 0L;

    /**
     * 构造号段模式 ID 生成器。
     *
     * <p>构造完成后不会立即加载号段，首次调用 {@link #nextId()} 时触发初始加载。
     *
     * @param dataSource 数据源（须已创建 {@code t_id_segment} 表并插入对应 bizTag 行）
     * @param bizTag     业务标签，与 {@code t_id_segment.biz_tag} 列一致，
     *                   不同业务使用不同标签实现 ID 空间隔离
     */
    public SegmentIdGenerator(DataSource dataSource, String bizTag) {
        this.dataSource = dataSource;
        this.bizTag = bizTag;
        log.info("SegmentIdGenerator initialized: bizTag={}", bizTag);
    }

    /**
     * 生成下一个 long 型唯一 ID（趋势递增）。
     *
     * <p>若当前号段未耗尽，直接返回 {@code current.getAndIncrement()}；
     * 若号段耗尽，调用 {@link #loadSegment()} 申请新号段后再分配。
     *
     * @return 趋势递增的唯一 ID
     * @throws IllegalStateException 数据库申请号段失败时抛出
     */
    @Override
    public long nextId() {
        long id = current.getAndIncrement();
        // 号段耗尽时申请新号段（使用 synchronized 保证只有一个线程进入 DB）
        if (id >= maxId) {
            synchronized (this) {
                // double-check：可能其他线程已完成号段加载
                id = current.getAndIncrement();
                if (id >= maxId) {
                    loadSegment();
                    id = current.getAndIncrement();
                }
            }
        }
        return id;
    }

    /**
     * 生成下一个唯一 ID 的字符串形式。
     *
     * @return 趋势递增的唯一 ID 字符串
     */
    @Override
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 从数据库申请新号段，更新内存中的 current 和 maxId。
     *
     * <p>执行两步 SQL：
     * <ol>
     *   <li>UPDATE：{@code max_id = max_id + step}（原子更新，防止并发争抢同一号段）</li>
     *   <li>SELECT：查询更新后的 max_id 和 step，计算新号段范围 {@code [max_id - step, max_id)}</li>
     * </ol>
     *
     * @throws IllegalStateException 数据库操作失败或 bizTag 不存在时抛出
     */
    private synchronized void loadSegment() {
        log.debug("SegmentIdGenerator loading new segment from DB, bizTag={}", bizTag);
        try (Connection conn = dataSource.getConnection()) {
            // Step 1: 原子更新 max_id，防止多节点争抢同一号段
            try (PreparedStatement updateStmt = conn.prepareStatement(UPDATE_SQL)) {
                updateStmt.setString(1, bizTag);
                int rows = updateStmt.executeUpdate();
                if (rows == 0) {
                    throw new IllegalStateException(
                            "No record found in t_id_segment for bizTag: " + bizTag
                                    + ". Please insert a row before using SegmentIdGenerator.");
                }
            }

            // Step 2: 查询新的 max_id 和 step，计算号段范围
            try (PreparedStatement selectStmt = conn.prepareStatement(SELECT_SQL)) {
                selectStmt.setString(1, bizTag);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException(
                                "Cannot read segment info from t_id_segment for bizTag: " + bizTag);
                    }
                    long newMaxId = rs.getLong("max_id");
                    int step = rs.getInt("step");
                    // 新号段范围：[newMaxId - step, newMaxId)
                    long newCurrent = newMaxId - step;
                    current.set(newCurrent);
                    maxId = newMaxId;
                    log.info("SegmentIdGenerator loaded new segment: bizTag={}, range=[{}, {})",
                            bizTag, newCurrent, newMaxId);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "SegmentIdGenerator failed to load segment from DB for bizTag: " + bizTag, ex);
        }
    }
}
