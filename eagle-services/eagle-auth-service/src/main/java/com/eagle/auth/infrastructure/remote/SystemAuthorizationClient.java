package com.eagle.auth.infrastructure.remote;

import com.eagle.auth.infrastructure.remote.dto.AuthorizationInfoDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 授权信息内部 API 客户端(调 eagle-system-service /internal/authorization/{accountId})。
 * <p>
 * 返回 {@code ResponseEntity}:system-service 未找到 User 时返回 204 No Content,
 * 调用方应通过 {@code getStatusCode().is2xxSuccessful() && getBody() != null} 判定。
 */
@HttpExchange("/internal/authorization")
public interface SystemAuthorizationClient {

    /** 按 accountId 查询授权信息。无 User 记录时 HTTP 204,body 为 null。 */
    @GetExchange("/{accountId}")
    ResponseEntity<AuthorizationInfoDto> findByAccountId(@PathVariable Long accountId);
}
