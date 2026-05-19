package com.eagle.system.auth.infrastructure.adapter;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.port.AuthorizationInfo;
import com.eagle.system.auth.domain.port.AuthorizationPort;
import com.eagle.system.auth.domain.repository.AccountRepository;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.infrastructure.security.BlacklistChecker;
import com.eagle.system.auth.infrastructure.security.ClientIpHolder;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Spring Security UserDetailsService 实现
 * <p>
 * 使用 auth 域的 {@link AccountRepository} 加载认证凭据，
 * 使用 auth 域的 {@link AuthorizationPort} 加载姓名 / 角色授权信息。
 * <p>
 * 依赖方向：auth 内部调用，无跨模块依赖（六边形架构 Driven Port）
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class EagleUserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AuthorizationPort authorizationPort;
    private final BlacklistChecker blacklistChecker;

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserDetails loadUserByUsername(@NonNull String username)
            throws UsernameNotFoundException {
        String ip = ClientIpHolder.get();
        // 1. IP 前置黑名单（账号未知）
        blacklistChecker.checkLogin(username, null, ip, null);

        // 2. 从 auth 域加载认证凭据
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);

        // 3. 账号已知后再校验 phone / accountId 维度
        blacklistChecker.checkLogin(username, account.getPhone(), ip, account.getId());

        // 4. 通过 AuthorizationPort 加载授权信息（姓名、角色码）
        AuthorizationInfo authInfo = authorizationPort
                .findAuthorizationInfo(account.getId())
                .orElse(AuthorizationInfo.empty());

        return new EagleUser(
                account.getId(),
                account.getUsername(),
                account.getPassword(),
                authInfo.name() != null ? authInfo.name() : account.getUsername(),
                account.getPhone(),
                account.getStatus() == AccountStatus.ACTIVE,
                true,
                true,
                account.getStatus() == AccountStatus.ACTIVE,
                authInfo.roleCodes().stream()
                        .<GrantedAuthority>map(code ->
                                new SimpleGrantedAuthority(SecurityConstants.ROLE_START + code))
                        .collect(Collectors.toList())
        );
    }
}
