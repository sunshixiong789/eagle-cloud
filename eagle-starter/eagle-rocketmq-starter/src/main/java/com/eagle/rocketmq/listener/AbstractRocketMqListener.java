package com.eagle.rocketmq.listener;

import com.alibaba.fastjson2.JSON;
import com.eagle.common.event.BaseEvent;
import com.eagle.rocketmq.exception.RocketMqErrorCode;
import com.eagle.rocketmq.properties.RocketMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

/**
 * RocketMQ 领域事件消费者抽象基类。
 *
 * <p>子类声明为 {@code @Component}，构造器必须将 {@link RocketMqProperties} 透传给本类
 * (通过 {@code super(rocketMqProperties)})，并实现三个抽象方法：
 * <ul>
 *   <li>{@link #getTopic()} — 监听的 Topic</li>
 *   <li>{@link #getEventClass()} — 事件类型</li>
 *   <li>{@link #handle(BaseEvent)} — 业务处理逻辑</li>
 * </ul>
 *
 * <h2>子类构造器示例</h2>
 * <pre>{@code
 * @Component
 * public class OrderCreatedConsumer extends AbstractRocketMqListener<OrderCreatedEvent> {
 *
 *     private final StockApplicationService stockService;
 *
 *     public OrderCreatedConsumer(RocketMqProperties props,
 *                                 StockApplicationService stockService) {
 *         super(props);
 *         this.stockService = stockService;
 *     }
 *     // ... getTopic / getEventClass / handle
 * }
 * }</pre>
 *
 * <h2>消费保障机制（三层）</h2>
 * <ol>
 *   <li><b>RocketMQ 自动重试</b>：{@code handle()} 抛出异常时返回 {@code FAILURE}，
 *       Broker 按递增间隔重新投递（默认最多 16 次）。</li>
 *   <li><b>重试告警</b>：投递次数达到 {@link #getRetryAlertThreshold()} 时，
 *       调用 {@link #onRetryAlert} 触发告警，可覆盖接入监控系统。</li>
 *   <li><b>死信队列（DLQ）</b>：超过最大重试后消息进入
 *       {@code %DLQ%{consumerGroup}}，由 {@link AbstractDlqListener} 兜底处理。</li>
 * </ol>
 *
 * @param <T> 事件类型，必须继承 {@link BaseEvent}
 * @author 孙士雄
 */
@Slf4j
public abstract class AbstractRocketMqListener<T extends BaseEvent> implements InitializingBean, DisposableBean {

    private final RocketMqProperties rocketMqProperties;

    private PushConsumer consumer;

    /**
     * 构造器注入 {@link RocketMqProperties}。子类须在自身构造器中通过 {@code super(rocketMqProperties)} 透传。
     *
     * @param rocketMqProperties RocketMQ 全局配置
     */
    protected AbstractRocketMqListener(RocketMqProperties rocketMqProperties) {
        this.rocketMqProperties = rocketMqProperties;
    }

    // -------------------------------------------------------------------------
    // 子类必须实现
    // -------------------------------------------------------------------------

    /**
     * 返回监听的 Topic 名称。
     *
     * @return Topic 名称
     */
    protected abstract String getTopic();

    /**
     * 返回事件类型 Class，用于反序列化。
     *
     * @return 事件 Class
     */
    protected abstract Class<T> getEventClass();

    /**
     * 处理已反序列化的事件。
     *
     * <p>方法抛出异常时消息返回 {@code FAILURE}，Broker 将按重试策略重新投递。
     * 务必保证实现的幂等性（以 {@code event.getEventId()} 为去重 key）。
     *
     * @param event 领域事件
     */
    protected abstract void handle(T event);

    // -------------------------------------------------------------------------
    // 子类可选覆盖
    // -------------------------------------------------------------------------

    /**
     * 返回接入点地址，默认使用全局配置 {@code eagle.rocketmq.endpoints}。
     *
     * @return 接入点地址
     */
    protected String getEndpoints() {
        return rocketMqProperties.getEndpoints();
    }

    /**
     * 返回消费者组，默认使用全局配置 {@code eagle.rocketmq.consumerGroup}。
     *
     * @return 消费者组名
     */
    protected String getConsumerGroup() {
        return rocketMqProperties.getConsumerGroup();
    }

    /**
     * 返回 Tag 过滤表达式，默认 {@code *}（接收所有 Tag）。
     *
     * <p>支持：
     * <ul>
     *   <li>{@code *} — 不过滤</li>
     *   <li>单个 Tag，如 {@code "ORDER_PAID"}</li>
     *   <li>多 Tag，如 {@code "ORDER_PAID || ORDER_CANCELLED"}</li>
     * </ul>
     *
     * @return Tag 过滤表达式
     */
    protected String getTagExpression() {
        return "*";
    }

    /**
     * 返回重试告警阈值，默认从 {@code eagle.rocketmq.consumer.retryAlertThreshold} 读取。
     *
     * <p>投递次数达到此值时触发 {@link #onRetryAlert}。
     * 可覆盖此方法设置每个消费者独立的阈值。
     *
     * @return 告警阈值（次数）
     */
    protected int getRetryAlertThreshold() {
        return rocketMqProperties.getConsumer().getRetryAlertThreshold();
    }

    /**
     * 消息消费重试次数达到告警阈值时的回调。
     *
     * <p>默认输出 ERROR 日志。可覆盖此方法接入告警平台（钉钉、邮件、PagerDuty 等）。
     * 告警后消息仍会继续重试，直到达到 Broker 的最大重试次数后进入 DLQ。
     *
     * @param messageView 消息视图（含 messageId、deliveryAttempt 等元信息）
     * @param rawBody     消息原始内容（用于告警时展示）
     * @param event       已反序列化的事件（反序列化失败时为 null）
     * @param cause       本次处理异常
     */
    protected void onRetryAlert(MessageView messageView, String rawBody, @Nullable T event, Exception cause) {
        log.error("[RETRY ALERT] Message has been retried {} times and still failing. " +
                        "topic: {}, group: {}, messageId: {}, eventId: {}. " +
                        "Will continue retry until max attempts, then enter DLQ.",
                messageView.getDeliveryAttempt(),
                messageView.getTopic(),
                getConsumerGroup(),
                messageView.getMessageId(),
                event != null ? event.getEventId() : "N/A",
                cause);
    }

    /**
     * 消息反序列化失败时的回调。
     *
     * <p>反序列化失败说明消息格式有误，重试不会有帮助，因此消息直接被 ACK（不进入重试）。
     * 默认输出 ERROR 日志。可覆盖此方法将消息持久化到异常消息表，供人工排查。
     *
     * @param messageView 消息视图
     * @param rawBody     原始消息内容
     * @param cause       反序列化异常
     */
    protected void onDeserializationFailed(MessageView messageView, String rawBody, Exception cause) {
        log.error("[DESERIALIZE FAILED] Message format error, message will be ACKed and dropped. " +
                        "topic: {}, messageId: {}, body: {}",
                messageView.getTopic(), messageView.getMessageId(), rawBody, cause);
    }

    // -------------------------------------------------------------------------
    // 生命周期
    // -------------------------------------------------------------------------

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            ClientConfiguration configuration = ClientConfiguration.newBuilder()
                    .setEndpoints(getEndpoints())
                    .setRequestTimeout(Duration.ofMillis(rocketMqProperties.getRequestTimeoutMillis()))
                    .enableSsl(rocketMqProperties.isSslEnabled())
                    .build();

            String tag = getTagExpression();
            FilterExpression filterExpression = "*".equals(tag)
                    ? new FilterExpression("*")
                    : new FilterExpression(tag, FilterExpressionType.TAG);

            RocketMqProperties.Consumer consumerConfig = rocketMqProperties.getConsumer();
            consumer = provider.newPushConsumerBuilder()
                    .setClientConfiguration(configuration)
                    .setConsumerGroup(getConsumerGroup())
                    .setSubscriptionExpressions(Collections.singletonMap(getTopic(), filterExpression))
                    .setMessageListener(this::onMessage)
                    .setMaxCacheMessageCount(consumerConfig.getMaxCachedMessageCount())
                    .setMaxCacheMessageSizeInBytes(consumerConfig.getMaxCachedMessageSizeInBytes())
                    .build();

            log.info("RocketMQ consumer started, topic: {}, group: {}, tag: {}, retryAlertThreshold: {}",
                    getTopic(), getConsumerGroup(), tag, getRetryAlertThreshold());
        } catch (ClientException e) {
            throw RocketMqErrorCode.CONSUMER_INIT_FAILED.toServiceException(e);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (consumer != null) {
            consumer.close();
            log.info("RocketMQ consumer closed, topic: {}", getTopic());
        }
    }

    // -------------------------------------------------------------------------
    // 消息处理（三层保障）
    // -------------------------------------------------------------------------

    private ConsumeResult onMessage(MessageView messageView) {
        String body = StandardCharsets.UTF_8.decode(messageView.getBody()).toString();
        int attempt = messageView.getDeliveryAttempt();

        // 重试时提升日志级别，首次消费保持 debug
        if (attempt > 1) {
            log.warn("Retrying message (attempt: {}/~{}), topic: {}, messageId: {}",
                    attempt, getRetryAlertThreshold(), messageView.getTopic(), messageView.getMessageId());
        } else {
            log.debug("Message received, topic: {}, messageId: {}",
                    messageView.getTopic(), messageView.getMessageId());
        }

        // ── 第一层：反序列化（格式错误无需重试，直接 ACK 丢弃）──────────────
        T event;
        try {
            event = JSON.parseObject(body, getEventClass());
        } catch (Exception e) {
            onDeserializationFailed(messageView, body, e);
            return ConsumeResult.SUCCESS;  // 消息格式问题，重试无意义
        }

        // ── 第二层：业务处理（失败触发 RocketMQ 重试）────────────────────────
        try {
            handle(event);

            // 经过重试才成功时打印恢复日志
            if (attempt > 1) {
                log.info("Message eventually consumed after {} retries, topic: {}, messageId: {}, eventId: {}",
                        attempt - 1, messageView.getTopic(), messageView.getMessageId(), event.getEventId());
            }
            return ConsumeResult.SUCCESS;

        } catch (Exception e) {
            // ── 第三层：告警（达到阈值，消息后续将进入 DLQ）──────────────────
            if (attempt >= getRetryAlertThreshold()) {
                onRetryAlert(messageView, body, event, e);
            } else {
                log.warn("Message handling failed, will retry (attempt: {}/~{}). " +
                                "topic: {}, messageId: {}, eventId: {}",
                        attempt, getRetryAlertThreshold(),
                        messageView.getTopic(), messageView.getMessageId(), event.getEventId(), e);
            }
            return ConsumeResult.FAILURE;  // 触发 Broker 重试
        }
    }
}
