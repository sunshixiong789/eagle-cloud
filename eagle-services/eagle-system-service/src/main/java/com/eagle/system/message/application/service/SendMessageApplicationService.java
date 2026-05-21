package com.eagle.system.message.application.service;

import com.eagle.system.message.domain.model.MessageCategory;
import com.eagle.system.message.domain.model.UserMessage;
import com.eagle.system.message.domain.repository.UserMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内消息发送应用服务。
 *
 * <p><strong>仅供本模块内部 Consumer 调用</strong>——其他业务模块/服务发送消息
 * 必须通过发布 {@code SendUserMessageIntegrationEvent} 集成事件触达，不要
 * 直接注入本服务（会引入跨模块耦合，违背模块拆分就绪原则）。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendMessageApplicationService {

    private final UserMessageRepository userMessageRepository;

    /**
     * 落库一条新消息。
     *
     * <p>带 bizKey 时按 bizKey 幂等去重；无 bizKey 时直接写入（调用方自行保证幂等，例如以 eventId 去重）。
     *
     * @return 新写入的消息；若 bizKey 已存在则返回 {@code null} 表示重复跳过
     */
    @Transactional
    @Nullable
    public UserMessage send(Long userId, MessageCategory category, String title,
                            String content, @Nullable String bizKey) {
        if (bizKey != null && !bizKey.isBlank() && userMessageRepository.existsByBizKey(bizKey)) {
            log.info("user-message duplicate skipped: bizKey={}, userId={}", bizKey, userId);
            return null;
        }
        UserMessage m = UserMessage.create(userId, category, title, content, bizKey);
        UserMessage saved = userMessageRepository.save(m);
        saved.registerCreatedEvent();
        log.info("user-message sent: id={}, userId={}, category={}, bizKey={}",
                saved.getId(), userId, category, bizKey);
        return saved;
    }
}
