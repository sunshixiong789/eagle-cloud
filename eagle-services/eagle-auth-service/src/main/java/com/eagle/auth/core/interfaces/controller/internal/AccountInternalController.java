package com.eagle.auth.core.interfaces.controller.internal;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account 内部 API（仅供 system-service 通过服务发现调用）。
 *
 * <p>主用途：system-service 启动期主动拉 admin Account 兜底，
 * 解决"AccountRegisteredEvent 只首次发布一次、MQ 不可用就永远拿不到"的强耦合。
 *
 * <p>路径前缀 {@code /internal/**} 由网关 + client-credentials scope 鉴权。
 *
 * @author sunshixiong
 */
@RestController
@RequestMapping("/internal/accounts")
@RequiredArgsConstructor
public class AccountInternalController {

    private final AccountRepository accountRepository;

    /**
     * 按用户名查 Account 快照。Account 不存在时返回 404（client 端 RestClient 错误处理器会
     * 自动转为 {@code NotFoundException}，调用方据此走 fallback 流程）。
     *
     * <p>仅暴露持久化字段——nickname / avatar / email 在 Account 上是
     * {@code @Transient ProfileHints}，注册事件已发出即被清除，此处取不到。
     */
    @GetMapping("/by-username/{username}")
    public AccountSnapshot findByUsername(@PathVariable String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);
        return new AccountSnapshot(account.getId(), account.getUsername(), account.getPhone());
    }

    /** 内部 Account 快照（仅持久化字段）。 */
    public record AccountSnapshot(Long accountId, String username, String phone) {
    }
}
