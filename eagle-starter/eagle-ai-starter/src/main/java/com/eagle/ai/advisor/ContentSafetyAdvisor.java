package com.eagle.ai.advisor;

import com.eagle.ai.exception.AiErrorCode;
import com.eagle.ai.properties.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.core.Ordered;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 内容安全过滤 Advisor。
 *
 * <p>拦截用户输入，按正则黑名单检测敏感内容，命中则拒绝请求（90004）。
 * 可选开启输出内容检查（{@code eagle.ai.safety.check-output=true}）。
 *
 * <p>黑名单正则在 Bean 初始化时预编译，运行期零额外开销。
 *
 * <p>配置示例：
 * <pre>{@code
 * eagle:
 *   ai:
 *     safety:
 *       enabled: true
 *       blocked-patterns:
 *         - "(?i)\\b(ignore previous|jailbreak|bypass safety)\\b"
 *         - "(?i)\\b(system prompt|internal prompt)\\b"
 *       check-output: false
 * }</pre>
 *
 * <p>仅在 {@code eagle.ai.safety.enabled=true} 时注册此 Bean。
 */
public class ContentSafetyAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyAdvisor.class);

    /** 在配额检查（HIGHEST_PRECEDENCE+200）之后运行，避免对不合规内容消耗配额。 */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 300;

    private final List<Pattern> compiledPatterns;
    private final boolean checkOutput;

    public ContentSafetyAdvisor(AiProperties properties) {
        AiProperties.Safety cfg = properties.getSafety();
        this.checkOutput = cfg.isCheckOutput();
        this.compiledPatterns = cfg.getBlockedPatterns().stream()
                .map(Pattern::compile)
                .toList();
    }

    @Override
    public String getName() {
        return "ContentSafetyAdvisor";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {
        String userText = extractUserText(request);
        if (isBlocked(userText)) {
            log.warn("Content safety violation detected in user input");
            throw AiErrorCode.AI_CONTENT_SAFETY_VIOLATION.toDomainException();
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain advisorChain) {
        if (!checkOutput) {
            return response;
        }
        String assistantText = extractAssistantText(response);
        if (isBlocked(assistantText)) {
            log.warn("Content safety violation detected in AI output");
            throw AiErrorCode.AI_CONTENT_SAFETY_VIOLATION.toServiceException();
        }
        return response;
    }

    // ==================== 内部工具 ====================

    private boolean isBlocked(String text) {
        if (text == null || text.isBlank() || compiledPatterns.isEmpty()) {
            return false;
        }
        return compiledPatterns.stream().anyMatch(p -> p.matcher(text).find());
    }

    private String extractUserText(ChatClientRequest request) {
        try {
            List<Message> instructions = request.prompt().getInstructions();
            if (instructions == null || instructions.isEmpty()) {
                return "";
            }
            return instructions.stream()
                    .filter(m -> m.getMessageType() == MessageType.USER)
                    .map(Message::getText)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining(" "));
        } catch (Exception e) {
            log.debug("Failed to extract user text for safety check", e);
            return "";
        }
    }

    private String extractAssistantText(ChatClientResponse response) {
        try {
            if (response.chatResponse() == null) {
                return "";
            }
            var result = response.chatResponse().getResult();
            if (result == null || result.getOutput() == null) {
                return "";
            }
            String text = result.getOutput().getText();
            return text != null ? text : "";
        } catch (Exception e) {
            log.debug("Failed to extract assistant text for safety check", e);
            return "";
        }
    }
}
