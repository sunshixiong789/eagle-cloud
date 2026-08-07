package com.eagle.amqp.support;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 记录 broker 未确认（nack）的发布。
 *
 * <p><b>与 {@link UnroutableMessageLogger} 的分工</b>——两者是<b>不同</b>的失败模式，缺一不可：
 * <ul>
 *   <li>return（unroutable）：broker <b>收到了</b>，但没有队列匹配 routing key</li>
 *   <li>nack（本类）：broker <b>压根没收下</b> —— 磁盘写满、内存告警、队列所在节点不可用</li>
 * </ul>
 *
 * <p><b>为什么必须记</b>：{@code spring.rabbitmq.publisher-confirm-type=correlated} 只是打开了
 * confirm 通道，不注册回调等于没开 —— {@code send()} 是即发即忘，不等确认就返回。
 * 而本项目的集成事件都在 {@code @Async @TransactionalEventListener(AFTER_COMMIT)} 里发出：
 * 数据库事务<b>已经提交</b>，此时消息没进 broker 就是永久丢失，且异步线程里的失败连调用栈都回不去。
 *
 * <p><b>本类只负责让失败可见，不负责补偿。</b> 要做到"发出去就一定不丢"，需要 outbox 表
 * （业务事务内落一行待发记录，发送成功后标记，定时扫描重投）—— 那是架构层面的改造。
 * 在此之前，这条 ERROR 日志是唯一能发现丢失的途径，务必接入告警。
 *
 * @author eagle
 */
@Slf4j
public class PublishConfirmLogger implements RabbitTemplate.ConfirmCallback {

    @Override
    public void confirm(@Nullable CorrelationData correlationData, boolean ack, @Nullable String cause) {
        if (ack) {
            return;
        }
        log.error("[AMQP NACK] broker did NOT accept the message, it is LOST: eventId={}, cause={}. "
                        + "上游事务已提交但事件未送达，需要人工补偿 —— "
                        + "常见原因：broker 磁盘/内存告警、镜像队列节点不可用。",
                correlationData == null ? "unknown" : correlationData.getId(),
                cause);
    }
}
