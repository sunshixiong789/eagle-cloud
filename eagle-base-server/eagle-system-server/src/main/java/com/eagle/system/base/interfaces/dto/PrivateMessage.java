package com.eagle.system.base.interfaces.dto;

import lombok.Data;

/**
 * 发送给个人的消息
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/29-18:03
 */
@Data
public class PrivateMessage {
    private String to;
    private String content;
    private String from;
}
