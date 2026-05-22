package com.eagle.system.base.interfaces.controller.internal;

import com.eagle.system.base.application.service.AuthorizationQueryService;
import com.eagle.system.base.interfaces.dto.response.AuthorizationView;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权信息相关的内部 API(仅供 auth-service 通过服务发现调用,
 * 用于构建 JWT claims 中的用户姓名和角色码)。
 *
 * <p>路径前缀 {@code /internal/**} 由网关 IP 白名单 + client-credentials
 * OAuth2 scope 鉴权(放行规则见 application.yml → {@code eagle.resource-server.permit-paths})。
 */
@RestController
@RequestMapping("/internal/authorization")
@RequiredArgsConstructor
public class AuthorizationInternalController {

    private final AuthorizationQueryService authorizationQueryService;

    /**
     * 按 accountId 查询授权信息。
     *
     * @return 用户存在返回 200 + AuthorizationView;不存在返回 204 No Content
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<AuthorizationView> findByAccountId(@PathVariable Long accountId) {
        return authorizationQueryService.findByAccountId(accountId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
