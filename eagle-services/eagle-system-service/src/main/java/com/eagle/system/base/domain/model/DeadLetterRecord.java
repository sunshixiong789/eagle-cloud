package com.eagle.system.base.domain.model;

import com.eagle.datajpa.base.BaseAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * MQ 死信记录 — 16 次重试仍失败的事件最终落库,
 * 供 oncall 人工补录、审计追溯、自动化补偿任务消费。
 *
 * <p>本聚合根有意"宽容":即便 event payload 反序列化异常或字段大小超预期,
 * 也要尽量记录 raw body + 元信息 + 异常文本,告警链路与落库链路独立失败不互相影响。
 *
 * <p>建表策略:当前 system-service 走 {@code ddl-auto=update}(dev) / 运维 DDL(prod),
 * 引入 Flyway 后由迁移脚本接管。
 *
 * @author sunshixiong
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "sys_dead_letter", indexes = {
        @Index(name = "idx_dlr_topic_status", columnList = "topic, status"),
        @Index(name = "idx_dlr_event_id", columnList = "event_id"),
        @Index(name = "idx_dlr_create_time", columnList = "create_time")
})
public class DeadLetterRecord extends BaseAggregateRoot<DeadLetterRecord> {

    /** 集成事件 eventId(UUID v7,来自 BaseEvent),允许为 null 防御反序列化失败场景。 */
    @Column(name = "event_id", length = 64, comment = "集成事件 eventId(UUID v7)")
    private String eventId;

    /** RocketMQ topic,例如 {@code eagle.auth.events}。 */
    @Column(nullable = false, length = 128, comment = "RocketMQ topic")
    private String topic;

    /** RocketMQ tag / 子分类,例如 {@code account.registered}。 */
    @Column(length = 64, comment = "RocketMQ tag / 业务子分类")
    private String tag;

    /** 原消费者组,用于人工补录时定位消费链路。 */
    @Column(name = "consumer_group", nullable = false, length = 128, comment = "原消费者组")
    private String consumerGroup;

    /** 投递累计次数(含首次)。 */
    @Column(name = "total_attempts", nullable = false, comment = "投递累计次数")
    private int totalAttempts;

    /** 事件 payload 序列化字符串(JSON),便于人工排查。 */
    @Column(nullable = false, columnDefinition = "TEXT", comment = "事件 payload(JSON)")
    private String payload;

    /** 异常文本(若可获取),便于人工排查根因。 */
    @Column(name = "error_message", columnDefinition = "TEXT", comment = "失败原因/异常文本")
    private String errorMessage;

    /** 状态:PENDING 待处理 / RESOLVED 已人工处理 / IGNORED 确认忽略。 */
    @Column(nullable = false, length = 16, comment = "处理状态")
    private String status;

    /** 处理时间,null 表示未处理。 */
    @Column(name = "resolved_at", comment = "处理时间")
    private LocalDateTime resolvedAt;

    /** 处理备注,人工补录的操作说明。 */
    @Column(name = "resolved_note", length = 500, comment = "处理备注")
    private String resolvedNote;

    private DeadLetterRecord(String eventId, String topic, String tag, String consumerGroup,
                             int totalAttempts, String payload, String errorMessage) {
        this.eventId = eventId;
        this.topic = topic;
        this.tag = tag;
        this.consumerGroup = consumerGroup;
        this.totalAttempts = totalAttempts;
        this.payload = payload;
        this.errorMessage = errorMessage;
        this.status = "PENDING";
    }

    /**
     * 工厂方法,所有字段均允许 null/空(防御反序列化失败场景)。
     */
    public static DeadLetterRecord capture(String eventId, String topic, String tag,
                                           String consumerGroup, int totalAttempts,
                                           String payload, String errorMessage) {
        return new DeadLetterRecord(eventId, topic, tag, consumerGroup,
                totalAttempts, payload, errorMessage);
    }

    /**
     * 标记为已人工处理。
     */
    public void resolve(String note) {
        this.status = "RESOLVED";
        this.resolvedAt = LocalDateTime.now();
        this.resolvedNote = note;
    }

    /**
     * 标记为确认忽略(例如确认是过期消息)。
     */
    public void ignore(String note) {
        this.status = "IGNORED";
        this.resolvedAt = LocalDateTime.now();
        this.resolvedNote = note;
    }
}
