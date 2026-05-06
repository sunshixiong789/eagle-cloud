package com.eagle.system.base.infrastructure.adapter;

import com.eagle.system.auth.domain.port.AuthorizationInfo;
import com.eagle.system.auth.domain.port.AuthorizationPort;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 授权信息查询适配器(Driven Adapter)
 * <p>
 * 实现 auth 域定义的 {@link AuthorizationPort} 接口，
 * 为 auth 域提供用户姓名信息（用于 JWT Token 构建）。
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationAdapter implements AuthorizationPort {

    private final UserRepository userRepository;

    @Override
    public Optional<AuthorizationInfo> findAuthorizationInfo(Long accountId) {
        return userRepository.findByAccountId(accountId)
                .map(this::toAuthorizationInfo);
    }

    private AuthorizationInfo toAuthorizationInfo(User user) {
        String name = user.getProfile() != null ? user.getProfile().getName() : null;
        return new AuthorizationInfo(name);
    }
}
