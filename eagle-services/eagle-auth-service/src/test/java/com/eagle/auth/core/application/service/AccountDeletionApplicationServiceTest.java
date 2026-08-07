package com.eagle.auth.core.application.service;

import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.AppleIdentityService;
import com.eagle.auth.core.config.AdminProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionApplicationServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AppleIdentityService appleIdentityService;
    @Mock
    private AccountApplicationService accountApplicationService;
    @Mock
    private AdminProperties adminProperties;
    @InjectMocks
    private AccountDeletionApplicationService service;

    @Test
    void revokesAppleAuthorizationBeforeDeletingLocalAccount() {
        Account account = Account.createFromApple(
                "apple-subject", null, "Apple 用户", "encrypted-refresh-token");
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));
        when(adminProperties.getUsername()).thenReturn("admin");

        service.deleteAccount(42L);

        InOrder order = inOrder(appleIdentityService, accountApplicationService);
        order.verify(appleIdentityService)
                .revokeEncryptedRefreshToken("encrypted-refresh-token");
        order.verify(accountApplicationService).deleteAccount(42L);
    }
}
