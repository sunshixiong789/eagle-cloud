package com.eagle.amqp;

import com.eagle.amqp.listener.AbstractAmqpListener;
import com.eagle.amqp.listener.AbstractDlqListener;
import com.eagle.amqp.properties.AmqpProperties;
import com.eagle.amqp.support.ExchangeNaming;
import com.eagle.common.event.BaseEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AMQP 拓扑命名")
class AmqpTopologyTest {

    private static final String TOPIC = "user_invitation_bound";

    private static AmqpProperties props(String prefix, String defaultGroup) {
        AmqpProperties p = new AmqpProperties();
        p.setExchangePrefix(prefix);
        p.setConsumerGroup(defaultGroup);
        return p;
    }

    /** 测试用消息载荷 */
    static class SampleMessage extends BaseEvent {
    }

    /** 覆盖了 consumerGroup 的消费者 */
    static class MembershipConsumer extends AbstractAmqpListener<SampleMessage> {
        MembershipConsumer(AmqpProperties p) {
            super(p);
        }

        @Override
        protected String getTopic() {
            return TOPIC;
        }

        @Override
        protected String getConsumerGroup() {
            return "user_membership_invitation_bound";
        }

        @Override
        protected Class<SampleMessage> getEventClass() {
            return SampleMessage.class;
        }

        @Override
        protected void handle(SampleMessage event) {
        }
    }

    /** 同一 topic 上的另一个消费者，独立 consumerGroup */
    static class MessageConsumer extends AbstractAmqpListener<SampleMessage> {
        MessageConsumer(AmqpProperties p) {
            super(p);
        }

        @Override
        protected String getTopic() {
            return TOPIC;
        }

        @Override
        protected String getConsumerGroup() {
            return "user_message_invitation_bound";
        }

        @Override
        protected Class<SampleMessage> getEventClass() {
            return SampleMessage.class;
        }

        @Override
        protected void handle(SampleMessage event) {
        }
    }

    /** 守护 MembershipConsumer 的 DLQ 消费者 */
    static class MembershipDlqListener extends AbstractDlqListener<SampleMessage> {
        MembershipDlqListener(AmqpProperties p) {
            super(p);
        }

        @Override
        protected String getOriginalTopic() {
            return TOPIC;
        }

        @Override
        protected String getOriginalConsumerGroup() {
            return "user_membership_invitation_bound";
        }

        @Override
        protected Class<SampleMessage> getEventClass() {
            return SampleMessage.class;
        }

        @Override
        protected void handleDeadLetter(SampleMessage event, int totalAttempts) {
        }
    }

    @Nested
    @DisplayName("exchange 与 queue 命名")
    class Naming {

        @Test
        @DisplayName("exchange 名应带环境前缀")
        void shouldPrefixExchangeName() {
            var consumer = new MembershipConsumer(props("dev_", "user_service_default"));
            assertThat(consumer.resolveExchangeName()).isEqualTo("dev_user_invitation_bound");
        }

        @Test
        @DisplayName("前缀为空时 exchange 名等于 topic 字面量")
        void shouldKeepLiteralWhenNoPrefix() {
            var consumer = new MembershipConsumer(props("", "user_service_default"));
            assertThat(consumer.resolveExchangeName()).isEqualTo(TOPIC);
        }

        @Test
        @DisplayName("queue 名应为 exchange + consumerGroup")
        void shouldComposeQueueName() {
            var consumer = new MembershipConsumer(props("dev_", "user_service_default"));
            assertThat(consumer.resolveQueueName())
                    .isEqualTo("dev_user_invitation_bound.user_membership_invitation_bound");
        }

        @Test
        @DisplayName("默认 routing key 是 # 而非 *（AMQP 语义与 RocketMQ 不同）")
        void shouldDefaultToHashRoutingKey() {
            var consumer = new MembershipConsumer(props("", "g"));
            assertThat(consumer.resolveRoutingKey()).isEqualTo("#");
            assertThat(ExchangeNaming.MATCH_ALL_ROUTING_KEY).isEqualTo("#");
        }
    }

    @Nested
    @DisplayName("竞争消费修复")
    class CompetingConsumptionFix {

        @Test
        @DisplayName("同 topic 的两个消费者应落到不同 queue，各自收全量")
        void shouldGiveEachConsumerItsOwnQueue() {
            AmqpProperties p = props("dev_", "user_service_default");
            var membership = new MembershipConsumer(p);
            var message = new MessageConsumer(p);

            // 同一个 exchange
            assertThat(membership.resolveExchangeName()).isEqualTo(message.resolveExchangeName());
            // 但 queue 必须不同 —— 这是修复竞争消费的关键
            assertThat(membership.resolveQueueName()).isNotEqualTo(message.resolveQueueName());
        }
    }

    @Nested
    @DisplayName("DLQ 命名")
    class Dlq {

        @Test
        @DisplayName("DLQ 名应为主 queue 名 + .dlq，与主消费者严格对齐")
        void shouldDeriveDlqFromMainQueue() {
            AmqpProperties p = props("dev_", "user_service_default");
            var main = new MembershipConsumer(p);
            var dlq = new MembershipDlqListener(p);

            assertThat(dlq.resolveDlqName()).isEqualTo(main.resolveQueueName() + ".dlq");
        }

        @Test
        @DisplayName("死信 exchange 名应为主 exchange + .dlx")
        void shouldDeriveDlxFromMainExchange() {
            var main = new MembershipConsumer(props("dev_", "g"));
            assertThat(ExchangeNaming.deadLetterExchange(main.resolveExchangeName()))
                    .isEqualTo("dev_user_invitation_bound.dlx");
        }
    }
}
