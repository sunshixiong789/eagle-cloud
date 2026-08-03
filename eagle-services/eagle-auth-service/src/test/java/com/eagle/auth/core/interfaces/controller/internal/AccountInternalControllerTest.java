package com.eagle.auth.core.interfaces.controller.internal;

import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.enums.AccountStatus;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.infrastructure.config.AdminProperties;
import com.eagle.common.exception.NotFoundException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountInternalController")
class AccountInternalControllerTest {

    private static final Long ACCOUNT_ID = 7L;
    private static final String PHONE = "17708080863";

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AdminProperties adminProperties;
    @Mock
    private Account account;
    @Mock
    private Account secondAccount;
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

    @Test
    @DisplayName("批量查询应忽略不存在账号并按 accountId 排序返回")
    void shouldReturnExistingSnapshotsInAccountIdOrder() {
        when(accountRepository.findAllById(Set.of(ACCOUNT_ID, 8L)))
                .thenReturn(List.of(secondAccount, account));
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getUsername()).thenReturn("17708080863");
        when(account.getPhone()).thenReturn("17708080863");
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(secondAccount.getId()).thenReturn(8L);
        when(secondAccount.getUsername()).thenReturn("18800000008");
        when(secondAccount.getPhone()).thenReturn("18800000008");
        when(secondAccount.getStatus()).thenReturn(AccountStatus.FROZEN);

        List<AccountInternalController.AccountSnapshot> snapshots = controller.findBatch(
                new AccountInternalController.AccountBatchRequest(Set.of(ACCOUNT_ID, 8L)));

        assertThat(snapshots).extracting(AccountInternalController.AccountSnapshot::accountId)
                .containsExactly(ACCOUNT_ID, 8L);
        assertThat(snapshots.get(1).locked()).isTrue();
    }

    @Test
    @DisplayName("批量请求超过 100 个账号应违反参数约束")
    void shouldRejectMoreThanOneHundredAccountIds() {
        Set<Long> ids = LongStream.rangeClosed(1, 101).boxed().collect(Collectors.toSet());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var violations = validator.validate(new AccountInternalController.AccountBatchRequest(ids));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("accountIds");
    }

    @Test
    @DisplayName("单个手机号命中应返回账号快照(单查复用批量端点,手机号不进 URL)")
    void shouldReturnSnapshotWhenPhoneMatches() {
        Set<String> phones = Set.of(PHONE);
        when(accountRepository.findByPhoneIn(phones)).thenReturn(List.of(account));
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getUsername()).thenReturn("phone_a1b2c3d4");
        when(account.getPhone()).thenReturn(PHONE);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);

        List<AccountInternalController.AccountSnapshot> snapshots = controller.findBatchByPhones(
                new AccountInternalController.AccountPhoneBatchRequest(phones));

        assertThat(snapshots).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.accountId()).isEqualTo(ACCOUNT_ID);
            assertThat(snapshot.username()).isEqualTo("phone_a1b2c3d4");
            assertThat(snapshot.phone()).isEqualTo(PHONE);
            assertThat(snapshot.locked()).isFalse();
        });
    }

    @Test
    @DisplayName("手机号未注册应返回空列表(调用方据此走挂起分支,不是异常路径)")
    void shouldReturnEmptyWhenPhoneAbsent() {
        Set<String> phones = Set.of(PHONE);
        when(accountRepository.findByPhoneIn(phones)).thenReturn(List.of());

        assertThat(controller.findBatchByPhones(
                new AccountInternalController.AccountPhoneBatchRequest(phones))).isEmpty();
    }

    @Test
    @DisplayName("按手机号批量查询应忽略未注册号码并按 accountId 排序返回")
    void shouldReturnExistingSnapshotsByPhonesInAccountIdOrder() {
        Set<String> phones = Set.of(PHONE, "18800000008");
        when(accountRepository.findByPhoneIn(phones)).thenReturn(List.of(secondAccount, account));
        when(account.getId()).thenReturn(ACCOUNT_ID);
        when(account.getUsername()).thenReturn("phone_a1b2c3d4");
        when(account.getPhone()).thenReturn(PHONE);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(secondAccount.getId()).thenReturn(8L);
        when(secondAccount.getUsername()).thenReturn("phone_e5f6a7b8");
        when(secondAccount.getPhone()).thenReturn("18800000008");
        when(secondAccount.getStatus()).thenReturn(AccountStatus.FROZEN);

        List<AccountInternalController.AccountSnapshot> snapshots = controller.findBatchByPhones(
                new AccountInternalController.AccountPhoneBatchRequest(phones));

        assertThat(snapshots).extracting(AccountInternalController.AccountSnapshot::accountId)
                .containsExactly(ACCOUNT_ID, 8L);
        assertThat(snapshots.get(1).locked()).isTrue();
    }

    @Test
    @DisplayName("按手机号批量请求超过 100 个应违反参数约束")
    void shouldRejectMoreThanOneHundredPhones() {
        Set<String> phones = IntStream.rangeClosed(1, 101)
                .mapToObj(i -> String.format("177%08d", i))
                .collect(Collectors.toSet());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        var violations = validator.validate(
                new AccountInternalController.AccountPhoneBatchRequest(phones));

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("phones");
    }
}
