package com.eagle.message.service;

import com.eagle.message.channel.MessageChannel;
import com.eagle.message.dto.MessageDTO;
import com.eagle.message.exception.MessageErrorCode;
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
     * @throws com.eagle.common.exception.ServiceException 渠道不支持或接收人为空时
     */
    public void send(MessageDTO message) {
        if (message.recipients().isEmpty()) {
            throw MessageErrorCode.EMPTY_RECIPIENTS.toServiceException();
        }
        // 模板不存在时提前失败，防止下游收到空消息
        if (!templateEngine.exists(message.templateCode())) {
            throw MessageErrorCode.TEMPLATE_NOT_FOUND.toServiceException(
                    (Object) message.templateCode());
        }

        String content = templateEngine.render(message.templateCode(), message.params());
        MessageChannel channel = channels.stream()
                .filter(c -> c.supports(message.channelType()))
                .findFirst()
                .orElseThrow(() -> MessageErrorCode.CHANNEL_NOT_SUPPORTED.toServiceException(
                        (Object) message.channelType().name()));

        channel.send(message, content);
        log.info("Message sent via {}, template: {}, recipients: {}",
                message.channelType(), message.templateCode(), message.recipients().size());
    }

    /**
     * 异步发送消息（使用专用线程池 {@code messageTaskExecutor}）。
     *
     * @param message 消息对象
     */
    @Async("messageTaskExecutor")
    public void sendAsync(MessageDTO message) {
        send(message);
    }
}