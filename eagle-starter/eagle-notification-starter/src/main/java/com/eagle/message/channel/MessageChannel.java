package com.eagle.message.channel;

import com.eagle.message.dto.MessageDTO;
import com.eagle.message.enums.MessageChannelType;

/**
 * 消息发送渠道抽象。
 *
 * @author 孙士雄
 */
public interface MessageChannel {

    /**
     * 判断是否支持指定的渠道类型。
     *
     * @param channelType 渠道类型
     * @return 是否支持
     */
    boolean supports(MessageChannelType channelType);

    /**
     * 发送消息。
     *
     * @param message         消息对象
     * @param renderedContent 渲染后的模板内容
     */
    void send(MessageDTO message, String renderedContent);
}
