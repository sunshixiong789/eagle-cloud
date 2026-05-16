package com.eagle.system.base.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发送给个人的消息
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/29-18:03
 */
@Data
@Schema(description = "WebSocket 私信(STOMP /message/message-to-one → 目标用户的 /user/queue/private)")
public class PrivateMessage {

    @Schema(description = "收件人用户 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private String to;

    @Schema(description = "消息正文", requiredMode = Schema.RequiredMode.REQUIRED, example = "你好")
    private String content;

    @Schema(description = "发送者标识(建议服务端基于 Principal 覆写防伪造)", example = "alice")
    private String from;
}
