package com.eagle.auth.interfaces.controller.internal;

import com.eagle.auth.domain.port.AccountBlacklistInfo;
import com.eagle.auth.domain.port.AccountBlacklistPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 账号黑名单相关的内部 API（仅供 system-service 通过服务发现调用）。
 *
 * <p>路径前缀 {@code /internal/**} 由网关 + client-credentials scope 鉴权。
 *
 * @author sunshixiong
 */
@RestController
@RequestMapping("/internal/account-blacklist")
@RequiredArgsConstructor
public class AccountBlacklistInternalController {

    private final AccountBlacklistPort accountBlacklistPort;

    /**
     * 查询某账号当前生效的黑名单记录。无记录时返回 204 No Content。
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountBlacklistInfo> findAccountBlacklist(@PathVariable Long accountId) {
        return accountBlacklistPort.findAccountBlacklist(accountId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
