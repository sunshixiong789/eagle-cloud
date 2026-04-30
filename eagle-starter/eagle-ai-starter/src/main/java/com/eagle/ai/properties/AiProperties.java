package com.eagle.ai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Eagle AI 配置属性。
 *
 * <p>所有属性均有默认值，无需额外配置即可启动。
 * 典型配置示例：
 * <pre>{@code
 * eagle:
 *   ai:
 *     chat:
 *       system-prompt: "你是一个专业的企业级助手，请用中文回答问题。"
 *       memory-window-size: 20
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "eagle.ai")
public class AiProperties {

    /** 是否启用 Eagle AI 自动配置（默认开启）。 */
    private boolean enabled = true;

    /** 对话（Chat）配置。 */
    private Chat chat = new Chat();

    /** Chat 相关配置。 */
    @Data
    public static class Chat {

        /**
         * 默认系统提示词（System Prompt）。
         * 不填则使用提供商默认行为；也可在调用时通过 {@code ChatClient.prompt().system(...)} 覆盖。
         */
        private String systemPrompt;

        /**
         * 对话记忆窗口大小 —— 每次请求携带的历史消息条数上限，默认 10。
         * 调大此值可保留更长上下文，但会增加 Token 消耗。
         */
        private int memoryWindowSize = 10;
    }
}
