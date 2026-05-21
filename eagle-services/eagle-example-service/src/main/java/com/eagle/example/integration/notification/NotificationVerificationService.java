package com.eagle.example.integration.notification;

import com.eagle.message.dto.MessageDTO;
import com.eagle.message.enums.MessageChannelType;
import com.eagle.message.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 消息通知 Starter 验证服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eagle.message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationVerificationService {

    private final NotificationService notificationService;

    public void sendSms(String phone, String templateCode, Map<String, String> params) {
        MessageDTO message = new MessageDTO(
                Set.of(phone),
                templateCode,
                params,
                MessageChannelType.SMS
        );
        notificationService.send(message);
        log.info("[Notification] SMS sent to {}", phone);
    }
}
