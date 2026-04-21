package com.eagle.system.system.web.dto;

import lombok.Data;

/**
 * 广播消息
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/29-18:03
 */
@Data
public class ChatMessage {
    private String content;
    private String sender;
}
