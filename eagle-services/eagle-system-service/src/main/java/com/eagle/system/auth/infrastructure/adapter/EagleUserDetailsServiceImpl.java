package com.eagle.system.auth.infrastructure.adapter;

import com.eagle.common.constant.SecurityConstants;
import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.domain.AuthErrorCode;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.model.enums.AccountStatus;
import com.eagle.system.auth.domain.port.AuthorizationInfo;
import com.eagle.system.auth.domain.port.AuthorizationPort;
import com.eagle.system.auth.domain.repository.AccountRepository;
import com.eagle.system.auth.infrastructure.security.BlacklistChecker;
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
 * Spring Security UserDetailsService 实现。
 *
 * <p>从 {@link AccountRepository} 取认证凭据，从 {@link AuthorizationPort} 取授权信息。
 * 各 grant Provider 在自动注册前已校验 IP / PHONE / OPENID 黑名单（早拒绝、省 DB）；
 * 本服务额外做一次 {@code ACCOUNT_ID} 黑名单校验——这是密码登录与所有自定义 grant
 * 签发 token 前共同的必经路径，在此一处即可拦截"账号被加黑后再次登录"。
 *
 * <p>依赖方向：auth 内部调用，无跨模块依赖（六边形架构 Driven Port）。
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
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);

        blacklistChecker.checkAccount(account.getId());

        AuthorizationInfo authInfo = authorizationPort
                .findAuthorizationInfo(account.getId())
                .orElse(AuthorizationInfo.empty());

        // 微信 / 短信 / 一键登录创建的账号密码字段为 {disabled} 占位，DAO 表单密码登录路径
        // 通过 PasswordEncoder.matches 注定失败，但仍保留 account.password 让自定义 grant 路径
        // 加载时同样使用此 UserDetails（自定义 grant 不走密码比对）。
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
