package com.eagle.system.auth.infrastructure.adapter;

import com.eagle.common.dto.EagleUser;
import com.eagle.system.auth.domain.model.Account;
import com.eagle.system.auth.domain.port.AuthorizationInfo;
import com.eagle.system.auth.domain.port.AuthorizationPort;
import com.eagle.system.auth.domain.repository.AccountRepository;
import com.eagle.system.auth.domain.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService 实现
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class EagleUserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;
    private final AuthorizationPort authorizationPort;

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserDetails loadUserByUsername(@NonNull String username)
            throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);

        AuthorizationInfo authInfo = authorizationPort
                .findAuthorizationInfo(account.getId())
                .orElse(AuthorizationInfo.empty());

        return new EagleUser(
                account.getId(),
                account.getUsername(),
                account.getPassword(),
                authInfo.name() != null ? authInfo.name() : account.getUsername(),
                null,
                null,
                account.getPhone(),
                !Boolean.TRUE.equals(account.getLocked()),
                true,
                true,
                !Boolean.TRUE.equals(account.getLocked()),
                AuthorityUtils.NO_AUTHORITIES
        );
    }
}
