package com.eagle.system.message.application.service;

import com.eagle.system.message.domain.model.UserMessage;
import com.eagle.system.message.domain.repository.UserMessageRepository;
import com.eagle.system.message.interfaces.dto.UserMessageResponse;
import com.eagle.system.message.domain.model.MessageErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站内消息查询/已读应用服务。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueryApplicationService {

    private final UserMessageRepository userMessageRepository;

    @Transactional(readOnly = true)
    public Page<UserMessageResponse> listMy(Long userId, Pageable pageable) {
        return userMessageRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable)
                .map(UserMessageResponse::of);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return userMessageRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long messageId) {
        UserMessage m = userMessageRepository.findById(messageId)
                .orElseThrow(() -> MessageErrorCode.MESSAGE_NOT_FOUND.toNotFoundException());
        m.assertOwnedBy(userId);
        if (!m.isRead()) {
            m.markRead();
            userMessageRepository.save(m);
        }
    }

    @Transactional
    public int markAllRead(Long userId) {
        int updated = userMessageRepository.markAllReadByUserId(userId);
        log.info("user-messages marked all read: userId={}, count={}", userId, updated);
        return updated;
    }
}
