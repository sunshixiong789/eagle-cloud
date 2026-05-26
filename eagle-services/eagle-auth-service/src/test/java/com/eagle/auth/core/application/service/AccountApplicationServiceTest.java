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
    private static final Long ACCOUNT_ID = 100L;

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SmsService smsService;
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
        @DisplayName("should save new account when username is free")
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
        @DisplayName("should throw conflict when username already exists")
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
        @DisplayName("should throw conflict when username already exists")
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
        @DisplayName("should update password via aggregate method and save")
        void shouldUpdatePassword() {
            Account account = existingAccount();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

            service.changePassword(ACCOUNT_ID, RAW_PASSWORD);

            assertEquals(ENCODED_PASSWORD, account.getPassword());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should throw NotFoundException when account not found")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.changePassword(ACCOUNT_ID, RAW_PASSWORD));
            assertEquals(AuthErrorCode.ACCOUNT_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("freezeAccount")
    class FreezeAccount {
        @Test
        @DisplayName("should freeze account and save")
        void shouldFreezeAccount() {
            Account account = existingAccount();
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
        @DisplayName("should throw when account not found")
        void shouldThrowWhenNotFound() {
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            assertThrows(NotFoundException.class, () -> service.freezeAccount(ACCOUNT_ID,
                    new com.eagle.auth.core.application.command.FreezeAccountCommand(
                            com.eagle.auth.core.domain.model.enums.FreezeReason.ADMIN,
                            null, null, 99L, "admin")));
        }
    }

    @Nested
    @DisplayName("unfreezeAccount")
    class UnfreezeAccount {
        @Test
        @DisplayName("should unfreeze and save")
        void shouldUnfreeze() {
            Account account = existingAccount();
            account.freezeByAdmin(99L, "admin",
                    com.eagle.auth.core.domain.model.enums.FreezeReason.ADMIN, null, null);
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.unfreezeAccount(ACCOUNT_ID, 99L, "admin");

            assertEquals(com.eagle.auth.core.domain.model.enums.AccountStatus.ACTIVE,
                    account.getStatus());
            verify(accountRepository).save(account);
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class Delete {

        @Test
        @DisplayName("should register deleted event and call repository.delete")
        void shouldRegisterEventAndDelete() {
            Account account = existingAccount();
            when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            service.deleteAccount(ACCOUNT_ID);

            verify(accountRepository).delete(account);
        }
    }

    @Nested
    @DisplayName("findOrCreateByPhone")
    class FindOrCreateByPhone {

        @Test
        @DisplayName("should return existing account when phone present")
        void shouldReturnExisting() {
            Account existing = existingAccount();
            when(accountRepository.findByPhone(PHONE)).thenReturn(Optional.of(existing));

            Account result = service.findOrCreateByPhone(PHONE);

            assertSame(existing, result);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create and save new account when phone not present")
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
    @DisplayName("authenticateBySmsCode")
    class AuthenticateBySmsCode {

        @Test
        @DisplayName("should throw when phone format invalid")
        void shouldThrowWhenPhoneInvalid() {
            AppException ex = assertThrows(DomainException.class,
                    () -> service.authenticateBySmsCode("123", "0000"));
            assertEquals(DataErrorCode.INVALID_PHONE_FORMAT, ex.getErrorCode());
            verify(smsService, never()).verifyCode(any(), any());
        }

        @Test
        @DisplayName("should throw when phone is null")
        void shouldThrowWhenPhoneNull() {
            assertThrows(DomainException.class,
                    () -> service.authenticateBySmsCode(null, "0000"));
        }

        @Test
        @DisplayName("should throw when sms code invalid")
        void shouldThrowWhenCodeInvalid() {
            when(smsService.verifyCode(PHONE, "wrong")).thenReturn(false);
            AppException ex = assertThrows(DomainException.class,
                    () -> service.authenticateBySmsCode(PHONE, "wrong"));
            assertEquals(AuthErrorCode.SMS_CODE_INVALID, ex.getErrorCode());
            verify(accountRepository, never()).findByPhone(any());
        }

        @Test
        @DisplayName("should return found-or-created account when phone and code valid")
        void shouldReturnAccount() {
            Account existing = existingAccount();
            when(smsService.verifyCode(PHONE, "1234")).thenReturn(true);
            when(accountRepository.findByPhone(PHONE)).thenReturn(Optional.of(existing));

            Account result = service.authenticateBySmsCode(PHONE, "1234");

            assertSame(existing, result);
        }
    }
}
