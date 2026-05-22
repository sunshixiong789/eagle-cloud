package com.eagle.auth.infrastructure.adapter;

import com.eagle.auth.domain.port.AuthorizationInfo;
import com.eagle.auth.domain.port.AuthorizationPort;
import com.eagle.auth.infrastructure.remote.SystemAuthorizationClient;
import com.eagle.auth.infrastructure.remote.dto.AuthorizationInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * {@link AuthorizationPort} 的远程实现:通过 RestClient 调 eagle-system-service
 * 的 {@code /internal/authorization/{accountId}} 内部端点,把姓名 + 角色码
 * 注入到 JWT claims。
 * <p>
 * 拆服务前是 system-service 内部的 AuthorizationAdapter(进程内 bean);
 * 拆分后由 system-service 暴露 HTTP 端点,这里转为远程调用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteAuthorizationAdapter implements AuthorizationPort {

    private final SystemAuthorizationClient systemAuthorizationClient;

    @Override
    public Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId) {
        ResponseEntity<AuthorizationInfoDto> resp;
        try {
            resp = systemAuthorizationClient.findByAccountId(accountId);
        } catch (RuntimeException ex) {
            log.warn("查询授权信息失败,accountId={},降级为 empty,reason={}",
                    accountId, ex.getMessage());
            return Optional.empty();
        }
        if (!resp.getStatusCode().is2xxSuccessful()) {
            return Optional.empty();
        }
        AuthorizationInfoDto body = resp.getBody();
        if (body == null) {
            return Optional.empty();
        }
        Set<String> roleCodes = body.roleCodes() != null ? body.roleCodes() : Set.of();
        return Optional.of(new AuthorizationInfo(body.name(), roleCodes));
    }
}
