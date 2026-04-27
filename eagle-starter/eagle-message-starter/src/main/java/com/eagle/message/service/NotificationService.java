package com.eagle.message.service;

import com.eagle.message.channel.MessageChannel;
import com.eagle.message.dto.MessageDTO;
import com.eagle.message.template.MessageTemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.util.List;

/**
 * 统一消息通知服务。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final MessageTemplateEngine templateEngine;
    private final List<MessageChannel> channels;

    /**
     * 同步发送消息。
     *
     * @param message 消息对象
     */
    public void send(MessageDTO message) {
        String content = templateEngine.render(message.templateCode(), message.params());
        MessageChannel channel = channels.stream()
                .filter(c -> c.supports(message.channelType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No channel supports: " + message.channelType()));
        channel.send(message, content);
        log.info("Message sent via {}, template: {}", message.channelType(), message.templateCode());
    }

    /**
     * 异步发送消息。
     *
     * @param message 消息对象
     */
    @Async("messageTaskExecutor")
    public void sendAsync(MessageDTO message) {
        send(message);
    }
}
