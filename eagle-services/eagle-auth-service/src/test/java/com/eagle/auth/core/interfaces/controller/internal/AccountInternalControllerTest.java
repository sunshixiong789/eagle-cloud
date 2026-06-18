package com.eagle.auth.core.interfaces.controller.internal;

import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.AccountStatus;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.infrastructure.config.AdminProperties;
import com.eagle.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountInternalController#findById")
class AccountInternalControllerTest {

    private static final Long ACCOUNT_ID = 7L;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AdminProperties adminProperties;
    @Mock
    private Account account;
    @InjectMocks
    private AccountInternalController controller;

    @Test
    @DisplayName("命中账号应返回 accountId/username/phone, FROZEN 映射 locked=true")
    void shouldReturnSnapshotWithLockedWhenFrozen() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getUsername()).thenReturn("17708080863");
        when(account.getPhone()).thenReturn("17708080863");
        when(account.getStatus()).thenReturn(AccountStatus.FROZEN);

        AccountInternalController.AccountSnapshot snapshot = controller.findById(ACCOUNT_ID);

        assertThat(snapshot.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(snapshot.username()).isEqualTo("17708080863");
        assertThat(snapshot.phone()).isEqualTo("17708080863");
        assertThat(snapshot.locked()).isTrue();
    }

    @Test
    @DisplayName("ACTIVE 账号 locked=false")
    void shouldReturnUnlockedWhenActive() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        assertThat(controller.findById(ACCOUNT_ID).locked()).isFalse();
    }

    @Test
    @DisplayName("账号不存在应抛 NotFoundException(下游 404)")
    void shouldThrowNotFoundWhenAbsent() {
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.findById(ACCOUNT_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
