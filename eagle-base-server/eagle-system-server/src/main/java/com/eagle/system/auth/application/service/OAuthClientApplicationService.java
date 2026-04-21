package com.eagle.system.auth.application.service;

import com.eagle.auth.application.mapper.OAuthClientMapper;
import com.eagle.auth.domain.model.OAuthClient;
import com.eagle.auth.domain.repository.OAuthClientRepository;
import com.eagle.auth.web.dto.request.CreateOAuthClientRequest;
import com.eagle.auth.web.dto.request.UpdateOAuthClientRequest;
import com.eagle.auth.web.dto.response.OAuthClientResponse;
import com.eagle.common.exception.codes.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * OAuth2 客户端应用服务
 * <p>
 * 编排领域对象完成客户端管理用例
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class OAuthClientApplicationService {

    private final OAuthClientRepository oAuthClientRepository;
    private final OAuthClientMapper oAuthClientMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建客户端
     *
     * @param request 创建请求
     * @return 客户端响应
     */
    @Transactional(rollbackFor = Exception.class)
    public OAuthClientResponse createClient(CreateOAuthClientRequest request) {
        if (oAuthClientRepository.existsByClientId(request.getClientId())) {
            throw AuthErrorCode.CLIENT_ID_EXISTS.toConflictException();
        }

        String encodedSecret = null;
        if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
            encodedSecret = passwordEncoder.encode(request.getClientSecret());
        }

        OAuthClient client = OAuthClient.create(
                request.getClientId(),
                encodedSecret,
                request.getClientName(),
                joinSet(request.getClientAuthenticationMethods()),
                joinSet(request.getAuthorizationGrantTypes()),
                joinSet(request.getRedirectUris()),
                joinSet(request.getScopes())
        );

        client.updateTokenSettings(
                request.getAccessTokenTtlSeconds(),
                request.getRefreshTokenTtlSeconds()
        );
        client.updateClientSettings(
                request.getRequireProofKey(),
                request.getRequireAuthorizationConsent()
        );

        OAuthClient saved = oAuthClientRepository.save(client);
        return oAuthClientMapper.toResponse(saved);
    }

    /**
     * 更新客户端
     *
     * @param id      客户端主键 ID
     * @param request 更新请求
     * @return 客户端响应
     */
    @Transactional(rollbackFor = Exception.class)
    public OAuthClientResponse updateClient(Long id, UpdateOAuthClientRequest request) {
        OAuthClient client = oAuthClientRepository.findById(id)
                .orElseThrow(AuthErrorCode.CLIENT_NOT_FOUND::toNotFoundException);

        String encodedSecret = null;
        if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
            encodedSecret = passwordEncoder.encode(request.getClientSecret());
        }

        client.updateInfo(
                request.getClientName(),
                encodedSecret,
                joinSet(request.getClientAuthenticationMethods()),
                joinSet(request.getAuthorizationGrantTypes()),
                joinSet(request.getRedirectUris()),
                joinSet(request.getScopes())
        );
        client.updateTokenSettings(
                request.getAccessTokenTtlSeconds(),
                request.getRefreshTokenTtlSeconds()
        );
        client.updateClientSettings(
                request.getRequireProofKey(),
                request.getRequireAuthorizationConsent()
        );

        OAuthClient saved = oAuthClientRepository.save(client);
        return oAuthClientMapper.toResponse(saved);
    }

    /**
     * 删除客户端
     *
     * @param id 客户端主键 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteClient(Long id) {
        if (!oAuthClientRepository.existsById(id)) {
            throw AuthErrorCode.CLIENT_NOT_FOUND.toNotFoundException();
        }
        oAuthClientRepository.deleteById(id);
    }

    /**
     * 查询单个客户端
     *
     * @param id 客户端主键 ID
     * @return 客户端响应
     */
    @Transactional(readOnly = true)
    public OAuthClientResponse getClientById(Long id) {
        OAuthClient client = oAuthClientRepository.findById(id)
                .orElseThrow(AuthErrorCode.CLIENT_NOT_FOUND::toNotFoundException);
        return oAuthClientMapper.toResponse(client);
    }

    /**
     * 分页查询客户端列表
     *
     * @param pageable 分页参数
     * @return 客户端分页响应
     */
    @Transactional(readOnly = true)
    public Page<OAuthClientResponse> queryClients(Pageable pageable) {
        return oAuthClientRepository.findAll(pageable)
                .map(oAuthClientMapper::toResponse);
    }

    /**
     * 启用客户端
     *
     * @param id 客户端主键 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableClient(Long id) {
        OAuthClient client = oAuthClientRepository.findById(id)
                .orElseThrow(AuthErrorCode.CLIENT_NOT_FOUND::toNotFoundException);
        client.enable();
        oAuthClientRepository.save(client);
    }

    /**
     * 禁用客户端
     *
     * @param id 客户端主键 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableClient(Long id) {
        OAuthClient client = oAuthClientRepository.findById(id)
                .orElseThrow(AuthErrorCode.CLIENT_NOT_FOUND::toNotFoundException);
        client.disable();
        oAuthClientRepository.save(client);
    }

    private String joinSet(Set<String> set) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        return String.join(",", set);
    }
}
