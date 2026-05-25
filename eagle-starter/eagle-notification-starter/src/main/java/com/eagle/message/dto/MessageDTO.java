package com.eagle.message.dto;

import com.eagle.message.enums.MessageChannelType;

import java.util.Map;
import java.util.Set;

/**
 * 消息发送数据传输对象。
 *
 * @author eagle
 */
public record MessageDTO(
        Set<String> recipients,
        String templateCode,
        Map<String, String> params,
        MessageChannelType channelType
) {
    public MessageDTO {
        recipients = recipients != null ? Set.copyOf(recipients) : Set.of();
        params = params != null ? Map.copyOf(params) : Map.of();
    }
}
