package com.eagle.amqp.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * 记录被 broker 退回的不可路由消息。
 *
 * <p><b>为什么必须有这一层</b>：AMQP 的默认行为是「发到没有任何队列绑定的 exchange = 静默丢弃」——
 * 生产方 {@code send()} 正常返回，没有异常、没有日志，消息就没了。
 * RocketMQ 不存在这个失败模式（topic 不存在会直接报错），所以迁移过来的代码天然没有防备。
 *
 * <p>真实事故：{@code SendUserMessageConsumer} 曾把环境前缀拼了两次，绑到
 * {@code dev_dev_user_message_send}，而生产方发往 {@code dev_user_message_send}。
 * 四类站内信（返利到账 / 提现到账 / 邀请绑定）长期全量丢失，健康检查全绿、日志无一行异常。
 * 有了本回调，这类拓扑错配在第一条消息发出时就会打出 ERROR。
 *
 * <p><b>生效前提</b>：连接工厂开启 {@code spring.rabbitmq.publisher-returns=true}，
 * 且发送方 {@code RabbitTemplate.setMandatory(true)} —— 两者由
 * {@code EagleAmqpAutoConfiguration} 的 customizer 一并设置。
 *
 * <p><b>不打印消息体</b>：消息载荷可能含手机号、昵称、站内信正文等个人信息，
 * 而错误日志的留存期通常远长于业务数据。定位用 {@code messageId}（即 {@code BaseEvent.eventId}）
 * 加 exchange / routingKey 已经足够，需要原文时按 eventId 去生产方日志捞。
 *
 * @author eagle
 */
@Slf4j
public class UnroutableMessageLogger implements RabbitTemplate.ReturnsCallback {

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.error("[AMQP UNROUTABLE] message returned by broker: "
                        + "exchange={}, routingKey={}, replyCode={}, replyText={}, messageId={}. "
                        + "该 exchange 上没有匹配 routingKey 的队列绑定 —— "
                        + "通常是消费方未启动、consumerGroup/topic 拼错，或环境前缀不一致。",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText(),
                returned.getMessage().getMessageProperties().getMessageId());
    }
}
