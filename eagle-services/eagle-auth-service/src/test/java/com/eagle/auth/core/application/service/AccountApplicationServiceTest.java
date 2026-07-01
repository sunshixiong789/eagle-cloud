package com.eagle.auth.core.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.codes.DataErrorCode;
import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import com.eagle.auth.core.domain.repository.AccountRepository;
import com.eagle.auth.core.domain.service.SmsService;
import com.eagle.auth.core.infrastructure.config.AdminProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    private static final String USERNAME = "alice";
    private static final String RAW_PASSWORD = "Pa$$w0rd!";
    private static final String ENCODED_PASSWORD = "{bcrypt}encoded";
    private static final String PHONE = "13800138000";
    private static final String TAOBAO_OPEN_UID = "tb_open_uid_abcdef0123456789";
    private static final Long ACCOUNT_ID = 100L;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SmsService smsService;
    @Mock
    private AdminProperties adminProperties;
    @InjectMocks
    private AccountApplicationService service;

    private Account existingAccount() {
        return Account.create(USERNAME, ENCODED_PASSWORD, PHONE,
                new ProfileHints("Alice", null, null));
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("用户名Free时应保存")
        void shouldSaveWhenUsernameFree() {
            when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            Account saved = existingAccount();
            when(accountRepository.save(any(Account.class))).thenReturn(saved);

            service.register(USERNAME, RAW_PASSWORD, PHONE, "alice@example.com", "Alice");

            ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(captor.capture());
            Account toSave = captor.getValue();
            assertEquals(USERNAME, toSave.getUsername());
            assertEquals(ENCODED_PASSWORD, toSave.getPassword());
            assertEquals(PHONE, toSave.getPhone());
        }

        @Test
        @DisplayName("用户名Exists时应抛出")
        void shouldThrowWhenUsernameExists() {
            when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existingAccount()));

            AppException ex = assertThrows(ConflictException.class,
                    () -> service.register(USERNAME, RAW_PASSWORD, PHONE, null, null));
            assertEquals(AuthErrorCode.ACCOUNT_ALREADY_EXISTS, ex.getErrorCode());
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("用户名Exists时应抛出")
        void shouldThrowWhenUsernameExists() {
            when(accountRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existingAccount()));
            AppException ex = assertThrows(ConflictException.class,
                    () -> service.createAccount(USERNAME, RAW_PASSWORD, PHONE, "n", "name", "e@x"));
            assertEquals(AuthErrorCode.ACCOUNT_ALREADY_EXISTS, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("应更新密码")
        void shouldUpdatePassword() {
            Account account = existingAccount();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            service.changePassword(ACCOUNT_ID, RAW_PASSWORD);

            assertEquals(ENCODED_PASSWORD, account.getPassword());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("账号不Found时应抛出")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.changePassword(ACCOUNT_ID, RAW_PASSWORD));
            assertEquals(AuthErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("管理员可修改自己的密码")
        void shouldAllowAdminToChangeOwnPassword() {
            Account admin = Account.create("admin", ENCODED_PASSWORD, PHONE, null);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(admin));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            service.changePassword(ACCOUNT_ID, RAW_PASSWORD);

            assertEquals(ENCODED_PASSWORD, admin.getPassword());
            verify(accountRepository).save(admin);
        }
    }

    @Nested
    @DisplayName("freezeAccount")
    class FreezeAccount {
        @Test
        @DisplayName("应冻结账号")
        void shouldFreezeAccount() {
            Account account = existingAccount();
            when(adminProperties.getUsername()).thenReturn("admin");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.freezeAccount(ACCOUNT_ID,
                    new com.eagle.auth.core.application.command.FreezeAccountCommand(
                            com.eagle.auth.core.domain.model.enums.FreezeReason.ADMIN,
                            null, "test", 99L, "admin"));

            assertEquals(com.eagle.auth.core.domain.model.enums.AccountStatus.FROZEN,
                    account.getStatus());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("不Found时应抛出")
        void shouldThrowWhenNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.freezeAccount(ACCOUNT_ID,
                    new com.eagle.auth.core.application.command.FreezeAccountCommand(
                            com.eagle.auth.core.domain.model.enums.FreezeReason.ADMIN,
                            null, null, 99L, "admin")));
        }

        @Test
        @DisplayName("应拒绝Freezing管理员账号")
        void shouldRejectFreezingAdminAccount() {
            Account admin = Account.create("admin", ENCODED_PASSWORD, PHONE, null);
            when(adminProperties.getUsername()).thenReturn("admin");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(admin));

            AppException ex = assertThrows(DomainException.class, () -> service.freezeAccount(ACCOUNT_ID,
                    new com.eagle.auth.core.application.command.FreezeAccountCommand(
                            com.eagle.auth.core.domain.model.enums.FreezeReason.ADMIN,
                            null, null, 99L, "operator")));

            assertEquals(AuthErrorCode.ADMIN_ACCOUNT_PROTECTED, ex.getErrorCode());
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unfreezeAccount")
    class UnfreezeAccount {
        @Test
        @DisplayName("应解冻")
        void shouldUnfreeze() {
            Account account = existingAccount();
            account.freezeByAdmin(99L, "admin",
                    com.eagle.auth.core.domain.model.enums.FreezeReason.ADMIN, null, null);
            when(adminProperties.getUsername()).thenReturn("admin");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.unfreezeAccount(ACCOUNT_ID, 99L, "admin");

            assertEquals(com.eagle.auth.core.domain.model.enums.AccountStatus.ACTIVE,
                    account.getStatus());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("应拒绝Unfreezing管理员账号")
        void shouldRejectUnfreezingAdminAccount() {
            Account admin = Account.create("admin", ENCODED_PASSWORD, PHONE, null);
            when(adminProperties.getUsername()).thenReturn("admin");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(admin));

            AppException ex = assertThrows(DomainException.class,
                    () -> service.unfreezeAccount(ACCOUNT_ID, 99L, "operator"));

            assertEquals(AuthErrorCode.ADMIN_ACCOUNT_PROTECTED, ex.getErrorCode());
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class Delete {

        @Test
        @DisplayName("应注册事件并删除")
        void shouldRegisterEventAndDelete() {
            Account account = existingAccount();
            when(adminProperties.getUsername()).thenReturn("admin");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            service.deleteAccount(ACCOUNT_ID);

            verify(accountRepository).delete(account);
        }

        @Test
        @DisplayName("应拒绝Deleting管理员账号")
        void shouldRejectDeletingAdminAccount() {
            Account admin = Account.create("admin", ENCODED_PASSWORD, PHONE, null);
            when(adminProperties.getUsername()).thenReturn("admin");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(admin));

            AppException ex = assertThrows(DomainException.class,
                    () -> service.deleteAccount(ACCOUNT_ID));

            assertEquals(AuthErrorCode.ADMIN_ACCOUNT_PROTECTED, ex.getErrorCode());
            verify(accountRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("findOrCreateByPhone")
    class FindOrCreateByPhone {

        @Test
        @DisplayName("应返回已有")
        void shouldReturnExisting() {
            Account existing = existingAccount();
            when(accountRepository.findByPhone(PHONE)).thenReturn(Optional.of(existing));

            Account result = service.findOrCreateByPhone(PHONE);

            assertSame(existing, result);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("应创建New")
        void shouldCreateNew() {
            when(accountRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            Account result = service.findOrCreateByPhone(PHONE);

            assertNotNull(result);
            assertEquals(PHONE, result.getPhone());
            verify(accountRepository, times(1)).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("findOrCreateByTaobao")
    class FindOrCreateByTaobao {

        @Test
        @DisplayName("应返回已有")
        void shouldReturnExisting() {
            Account existing = existingAccount();
            when(accountRepository.findByTaobaoBindingOpenUid(TAOBAO_OPEN_UID))
                    .thenReturn(Optional.of(existing));

            Account result = service.findOrCreateByTaobao(TAOBAO_OPEN_UID);

            assertSame(existing, result);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("应创建New，无需手机号")
        void shouldCreateNewWithoutPhone() {
            when(accountRepository.findByTaobaoBindingOpenUid(TAOBAO_OPEN_UID))
                    .thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

            Account result = service.findOrCreateByTaobao(TAOBAO_OPEN_UID);

            assertNotNull(result);
            assertEquals(TAOBAO_OPEN_UID, result.getTaobaoBinding().getOpenUid());
            verify(accountRepository, times(1)).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("changePhone")
    class ChangePhone {

        private static final String NEW_PHONE = "13900139000";
        private static final String CODE = "123456";

        @Test
        @DisplayName("校验通过时应替换并保存")
        void shouldChangePhone() {
            when(smsService.verifyCode(NEW_PHONE, CODE)).thenReturn(true);
            when(accountRepository.findByPhone(NEW_PHONE)).thenReturn(Optional.empty());
            Account account = existingAccount();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            service.changePhone(ACCOUNT_ID, NEW_PHONE, CODE);

            assertEquals(NEW_PHONE, account.getPhone());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("验证码错误时应抛 SMS_CODE_INVALID")
        void shouldThrowWhenCodeInvalid() {
            when(smsService.verifyCode(NEW_PHONE, CODE)).thenReturn(false);

            AppException ex = assertThrows(DomainException.class,
                    () -> service.changePhone(ACCOUNT_ID, NEW_PHONE, CODE));
            assertEquals(AuthErrorCode.SMS_CODE_INVALID, ex.getErrorCode());
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("新号已绑其他账号时应抛 PHONE_ALREADY_BOUND")
        void shouldThrowWhenBoundToOther() {
            when(smsService.verifyCode(NEW_PHONE, CODE)).thenReturn(true);
            Account other = existingAccount();
            org.springframework.test.util.ReflectionTestUtils.setField(other, "id", 999L);
            when(accountRepository.findByPhone(NEW_PHONE)).thenReturn(Optional.of(other));

            AppException ex = assertThrows(ConflictException.class,
                    () -> service.changePhone(ACCOUNT_ID, NEW_PHONE, CODE));
            assertEquals(AuthErrorCode.PHONE_ALREADY_BOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("新号与当前号相同时应抛 PHONE_NOT_CHANGED")
        void shouldThrowWhenUnchanged() {
            when(smsService.verifyCode(PHONE, CODE)).thenReturn(true);
            Account account = existingAccount();   // 当前 phone == PHONE
            org.springframework.test.util.ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
            when(accountRepository.findByPhone(PHONE)).thenReturn(Optional.of(account));
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            AppException ex = assertThrows(DomainException.class,
                    () -> service.changePhone(ACCOUNT_ID, PHONE, CODE));
            assertEquals(AuthErrorCode.PHONE_NOT_CHANGED, ex.getErrorCode());
        }

        @Test
        @DisplayName("账号冻结时应抛 ACCOUNT_FROZEN")
        void shouldThrowWhenFrozen() {
            when(smsService.verifyCode(NEW_PHONE, CODE)).thenReturn(true);
            when(accountRepository.findByPhone(NEW_PHONE)).thenReturn(Optional.empty());
            Account account = existingAccount();
            account.freezeByAdmin(1L, "admin",
                    com.eagle.auth.core.domain.model.enums.FreezeReason.OTHER, null, "test");
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            AppException ex = assertThrows(DomainException.class,
                    () -> service.changePhone(ACCOUNT_ID, NEW_PHONE, CODE));
            assertEquals(AuthErrorCode.ACCOUNT_FROZEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("并发触发唯一约束冲突时应翻译为 PHONE_ALREADY_BOUND")
        void shouldTranslateUniqueViolationToConflict() {
            when(smsService.verifyCode(NEW_PHONE, CODE)).thenReturn(true);
            when(accountRepository.findByPhone(NEW_PHONE)).thenReturn(Optional.empty());
            Account account = existingAccount();
            org.springframework.test.util.ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(account)).thenThrow(
                    new org.springframework.dao.DataIntegrityViolationException("uk_account_phone"));

            AppException ex = assertThrows(ConflictException.class,
                    () -> service.changePhone(ACCOUNT_ID, NEW_PHONE, CODE));
            assertEquals(AuthErrorCode.PHONE_ALREADY_BOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("authenticateBySmsCode")
    class AuthenticateBySmsCode {

        @Test
        @DisplayName("手机号无效时应抛出")
        void shouldThrowWhenPhoneInvalid() {
            AppException ex = assertThrows(DomainException.class,
                    () -> service.authenticateBySmsCode("123", "0000"));
            assertEquals(DataErrorCode.INVALID_PHONE_FORMAT, ex.getErrorCode());
            verify(smsService, never()).verifyCode(any(), any());
        }

        @Test
        @DisplayName("手机号null时应抛出")
        void shouldThrowWhenPhoneNull() {
            assertThrows(DomainException.class,
                    () -> service.authenticateBySmsCode(null, "0000"));
        }

        @Test
        @DisplayName("验证码无效时应抛出")
        void shouldThrowWhenCodeInvalid() {
            when(smsService.verifyCode(PHONE, "wrong")).thenReturn(false);
            AppException ex = assertThrows(DomainException.class,
                    () -> service.authenticateBySmsCode(PHONE, "wrong"));
            assertEquals(AuthErrorCode.SMS_CODE_INVALID, ex.getErrorCode());
            verify(accountRepository, never()).findByPhone(any());
        }

        @Test
        @DisplayName("应返回账号")
        void shouldReturnAccount() {
            Account existing = existingAccount();
            when(smsService.verifyCode(PHONE, "1234")).thenReturn(true);
            when(accountRepository.findByPhone(PHONE)).thenReturn(Optional.of(existing));

            Account result = service.authenticateBySmsCode(PHONE, "1234");

            assertSame(existing, result);
        }
    }
}
