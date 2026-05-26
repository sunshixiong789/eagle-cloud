package com.eagle.auth.core.infrastructure.remote;

import com.eagle.auth.core.infrastructure.remote.dto.AuthorizationInfoDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 授权信息内部 API 客户端(调 eagle-system-service {@code GET /internal/authorization/{accountId}})。
 *
 * <p><strong>返回语义</strong>:
 * <ul>
 *   <li>200 OK + body — 正常返回 {@link AuthorizationInfoDto}</li>
 *   <li>404 Not Found — system-service 找不到对应 User,starter 的 {@code EagleResponseErrorHandler}
 *       会抛 {@code NotFoundException},调用方应捕获并视为"账号无关联 user"</li>
 *   <li>其他状态码/网络异常 — 抛 {@code ServiceException}/{@code RuntimeException},
 *       调用方按熔断/缓存策略降级</li>
 * </ul>
 */
@HttpExchange("/internal/authorization")
public interface SystemAuthorizationClient {

    /** 按 accountId 查询授权信息。无 User 记录时下游返回 HTTP 404 → NotFoundException。 */
    @GetExchange("/{accountId}")
    AuthorizationInfoDto findByAccountId(@PathVariable Long accountId);
}
