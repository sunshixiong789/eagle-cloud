package com.eagle.system.base.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 广播消息
 *
 * @author eagle（sunshix@seeyon.com）
 * 2025/12/29-18:03
 */
@Schema(description = "WebSocket 广播消息(STOMP /message/broadcast-message → /topic/public)")
public record ChatMessage(

        @Schema(description = "消息正文", requiredMode = Schema.RequiredMode.REQUIRED, example = "大家好")
        String content,

        @Schema(description = "发送者标识(可选,服务端可基于 Principal 覆写)", example = "alice")
        String sender
) {
}
