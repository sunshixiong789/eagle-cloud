package com.eagle.system.base.infrastructure.remote;

import com.eagle.system.base.infrastructure.remote.dto.AccountBlacklistSnapshot;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 账号黑名单内部 API 客户端(调 auth-service /internal/account-blacklist/{accountId})。
 * <p>
 * 返回 {@code ResponseEntity}:无黑名单记录时 auth-service 返回 204 No Content,
 * 调用方应通过 {@code getStatusCode().is2xxSuccessful() && getBody() != null} 判定。
 */
@HttpExchange("/internal/account-blacklist")
public interface AuthAccountBlacklistClient {

    /** 查询某账号当前生效的黑名单记录。无记录时 HTTP 204,body 为 null。 */
    @GetExchange("/{accountId}")
    ResponseEntity<AccountBlacklistSnapshot> findByAccountId(@PathVariable Long accountId);
}
