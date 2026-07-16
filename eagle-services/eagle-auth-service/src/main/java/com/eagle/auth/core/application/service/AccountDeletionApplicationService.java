package com.eagle.auth.core.application.service;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.valueobject.AppleBinding;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.AppleIdentityService;
import com.eagle.auth.core.infrastructure.config.AdminProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 编排第三方授权撤销与本地账号删除，远程调用不进入数据库事务。 */
@Service
@RequiredArgsConstructor
public class AccountDeletionApplicationService {

    private final AccountRepository accountRepository;
    private final AppleIdentityService appleIdentityService;
    private final AccountApplicationService accountApplicationService;
    private final AdminProperties adminProperties;

    public void deleteAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(AuthErrorCode.ACCOUNT_NOT_FOUND::toNotFoundException);
        if (account.getUsername().equals(adminProperties.getUsername())) {
            throw AuthErrorCode.ADMIN_ACCOUNT_PROTECTED.toDomainException();
        }

        AppleBinding appleBinding = account.getAppleBinding();
        if (appleBinding != null
                && appleBinding.getRefreshTokenCiphertext() != null
                && !appleBinding.getRefreshTokenCiphertext().isBlank()) {
            appleIdentityService.revokeEncryptedRefreshToken(
                    appleBinding.getRefreshTokenCiphertext());
        }
        accountApplicationService.deleteAccount(accountId);
    }
}
