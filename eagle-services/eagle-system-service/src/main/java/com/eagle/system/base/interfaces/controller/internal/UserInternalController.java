package com.eagle.system.base.interfaces.controller.internal;

import com.eagle.system.base.application.service.AccountEventApplicationService;
import com.eagle.system.base.infrastructure.messaging.event.AccountRegisteredMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * User 内部 API(仅供 auth-service 通过服务发现调用)。
 *
 * <p>核心用途:在 RocketMQ 不可用时,作为 {@code AuthIntegrationEventPublisher.onAccountRegistered}
 * 的同步 HTTP 降级通道,保证 auth 账号注册事件不因 broker 抖动而永久丢失。
 *
 * <p>幂等等价于 MQ 消费路径:复用 {@link AccountEventApplicationService#onAccountRegistered},
 * 内部走 {@code UserRepository.existsByAccountId} + DB 唯一索引双重保护。重复调用安全。
 *
 * <p>路径前缀 {@code /internal/**} 由网关 IP 白名单 + client-credentials OAuth2 scope 鉴权。
 *
 * @author sunshixiong
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final AccountEventApplicationService accountEventService;

    /**
     * 从 Account 注册事件同步创建 User(MQ 降级通道)。
     *
     * <p>body 字段对齐 {@link AccountRegisteredMessage} —— 调用方(auth)用相同 JSON 形态
     * 即可,无需独立 DTO。
     *
     * @param message 与 RocketMQ 消息体同构的账号注册事件
     */
    @PostMapping("/from-account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void syncFromAccount(@RequestBody AccountRegisteredMessage message) {
        log.info("HTTP fallback sync user, accountId={}, username={}",
                message.getAccountId(), message.getUsername());
        accountEventService.onAccountRegistered(message);
    }
}
