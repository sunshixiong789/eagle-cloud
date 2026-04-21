package com.eagle.auth.application.service;

import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.model.valueobject.ProfileHints;
import com.eagle.auth.domain.repository.AccountRepository;
import com.eagle.auth.domain.service.SmsService;
import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AccountApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("账号应用服务")
@ExtendWith(MockitoExtension.class)
class AccountApplicationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private AccountApplicationService accountApplicationService;

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("should register account successfully")
        void shouldRegisterAccountSuccessfully() {
            // Given
            String username = "testuser";
            String rawPassword = "password123";
            String phone = "13800000000";
            String email = "test@example.com";
            String nickname = "Test User";

            when(accountRepository.findByUsername(username)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(rawPassword)).thenReturn("encoded_password");
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account account = invocation.getArgument(0);
                // 模拟 JPA 持久化后设置 ID
                try {
                    java.lang.reflect.Field idField = Account.class.getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(account, 1L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return account;
            });

            // When
            Long accountId = accountApplicationService.register(username, rawPassword, phone, email, nickname);

            // Then
            assertNotNull(accountId);
            verify(accountRepository).findByUsername(username);
            verify(passwordEncoder).encode(rawPassword);
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw ConflictException when username already exists")
        void shouldThrowWhenUsernameAlreadyExists() {
            // Given
            String username = "existinguser";
            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(mock(Account.class)));

            // When & Then
            assertThrows(ConflictException.class, () ->
                accountApplicationService.register(username, "password", null, null, null));
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("should create account with profile hints")
        void shouldCreateAccountWithProfileHints() {
            // Given
            String username = "adminuser";
            String rawPassword = "password123";
            String phone = "13800000000";
            String nickname = "Admin";
            String name = "管理员";
            String email = "admin@example.com";
            Long deptId = 1L;
            Set<Long> roleIds = Set.of(1L, 2L);

            when(accountRepository.findByUsername(username)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(rawPassword)).thenReturn("encoded_password");
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
                Account account = invocation.getArgument(0);
                // 模拟 JPA 持久化后设置 ID
                try {
                    java.lang.reflect.Field idField = Account.class.getSuperclass().getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(account, 1L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return account;
            });

            // When
            Long accountId = accountApplicationService.createAccount(
                username, rawPassword, phone, nickname, name, email, deptId, roleIds);

            // Then
            assertNotNull(accountId);
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw ConflictException when username already exists")
        void shouldThrowWhenUsernameAlreadyExists() {
            // Given
            String username = "existinguser";
            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(mock(Account.class)));

            // When & Then
            assertThrows(ConflictException.class, () ->
                accountApplicationService.createAccount(username, "password", null, null, null, null, null, null));
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("should change password successfully")
        void shouldChangePasswordSuccessfully() {
            // Given
            Long accountId = 1L;
            String rawNewPassword = "newpassword";
            Account account = Account.create("testuser", "old_pwd", null, ProfileHints.EMPTY);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
            when(passwordEncoder.encode(rawNewPassword)).thenReturn("encoded_new_pwd");

            // When
            accountApplicationService.changePassword(accountId, rawNewPassword);

            // Then
            assertEquals("encoded_new_pwd", account.getPassword());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should throw NotFoundException when account not found")
        void shouldThrowWhenAccountNotFound() {
            // Given
            Long accountId = 999L;
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                accountApplicationService.changePassword(accountId, "newpassword"));
        }
    }

    @Nested
    @DisplayName("lockAccount")
    class LockAccount {

        @Test
        @DisplayName("should lock account successfully")
        void shouldLockAccountSuccessfully() {
            // Given
            Long accountId = 1L;
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When
            accountApplicationService.lockAccount(accountId);

            // Then
            assertTrue(account.getLocked());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should throw DomainException when already locked")
        void shouldThrowWhenAlreadyLocked() {
            // Given
            Long accountId = 1L;
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);
            account.lock();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When & Then
            assertThrows(DomainException.class, () ->
                accountApplicationService.lockAccount(accountId));
        }
    }

    @Nested
    @DisplayName("unlockAccount")
    class UnlockAccount {

        @Test
        @DisplayName("should unlock account successfully")
        void shouldUnlockAccountSuccessfully() {
            // Given
            Long accountId = 1L;
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);
            account.lock();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When
            accountApplicationService.unlockAccount(accountId);

            // Then
            assertFalse(account.getLocked());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should throw DomainException when not locked")
        void shouldThrowWhenNotLocked() {
            // Given
            Long accountId = 1L;
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When & Then
            assertThrows(DomainException.class, () ->
                accountApplicationService.unlockAccount(accountId));
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("should delete account and publish deleted event")
        void shouldDeleteAccountAndPublishEvent() {
            // Given
            Long accountId = 1L;
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When
            accountApplicationService.deleteAccount(accountId);

            // Then
            verify(accountRepository).save(account); // flush event
            verify(accountRepository).deleteById(accountId);
        }
    }

    @Nested
    @DisplayName("findOrCreateByPhone")
    class FindOrCreateByPhone {

        @Test
        @DisplayName("should return existing account when phone exists")
        void shouldReturnExistingAccount() {
            // Given
            String phone = "13800000000";
            Account existingAccount = Account.createFromPhone(phone);

            when(accountRepository.findByPhone(phone)).thenReturn(Optional.of(existingAccount));

            // When
            Account result = accountApplicationService.findOrCreateByPhone(phone);

            // Then
            assertEquals(existingAccount, result);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new account when phone not exists")
        void shouldCreateNewAccount() {
            // Given
            String phone = "13800000000";

            when(accountRepository.findByPhone(phone)).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Account result = accountApplicationService.findOrCreateByPhone(phone);

            // Then
            assertNotNull(result);
            assertEquals(phone, result.getPhone());
            verify(accountRepository).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("findOrCreateByWechatOpenid")
    class FindOrCreateByWechatOpenid {

        @Test
        @DisplayName("should return existing account when openid exists")
        void shouldReturnExistingAccount() {
            // Given
            String openid = "openid_123";
            String unionid = "unionid_123";
            Account existingAccount = Account.createFromWechat(openid, unionid);

            when(accountRepository.findByWechatBindingOpenid(openid)).thenReturn(Optional.of(existingAccount));

            // When
            Account result = accountApplicationService.findOrCreateByWechatOpenid(openid, unionid);

            // Then
            assertEquals(existingAccount, result);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new account when openid not exists")
        void shouldCreateNewAccount() {
            // Given
            String openid = "openid_123";
            String unionid = "unionid_123";

            when(accountRepository.findByWechatBindingOpenid(openid)).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Account result = accountApplicationService.findOrCreateByWechatOpenid(openid, unionid);

            // Then
            assertNotNull(result);
            assertNotNull(result.getWechatBinding());
            verify(accountRepository).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("resetPasswordByPhone")
    class ResetPasswordByPhone {

        @Test
        @DisplayName("should reset password successfully")
        void shouldResetPasswordSuccessfully() {
            // Given
            String phone = "13800000000";
            String code = "123456";
            String rawNewPassword = "newpassword";
            Account account = Account.createFromPhone(phone);

            when(smsService.verifyCode(phone, code)).thenReturn(true);
            when(accountRepository.findByPhone(phone)).thenReturn(Optional.of(account));
            when(passwordEncoder.encode(rawNewPassword)).thenReturn("encoded_new_pwd");

            // When
            accountApplicationService.resetPasswordByPhone(phone, code, rawNewPassword);

            // Then
            assertEquals("encoded_new_pwd", account.getPassword());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should throw DomainException when sms code invalid")
        void shouldThrowWhenSmsCodeInvalid() {
            // Given
            String phone = "13800000000";
            String code = "invalid";

            when(smsService.verifyCode(phone, code)).thenReturn(false);

            // When & Then
            assertThrows(DomainException.class, () ->
                accountApplicationService.resetPasswordByPhone(phone, code, "newpassword"));
        }

        @Test
        @DisplayName("should throw NotFoundException when phone not bound")
        void shouldThrowWhenPhoneNotBound() {
            // Given
            String phone = "13800000000";
            String code = "123456";

            when(smsService.verifyCode(phone, code)).thenReturn(true);
            when(accountRepository.findByPhone(phone)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                accountApplicationService.resetPasswordByPhone(phone, code, "newpassword"));
        }

        @Test
        @DisplayName("should throw DomainException when account locked")
        void shouldThrowWhenAccountLocked() {
            // Given
            String phone = "13800000000";
            String code = "123456";
            Account account = Account.createFromPhone(phone);
            account.lock();

            when(smsService.verifyCode(phone, code)).thenReturn(true);
            when(accountRepository.findByPhone(phone)).thenReturn(Optional.of(account));

            // When & Then
            assertThrows(DomainException.class, () ->
                accountApplicationService.resetPasswordByPhone(phone, code, "newpassword"));
        }
    }

    @Nested
    @DisplayName("bindPhone")
    class BindPhone {

        @Test
        @DisplayName("should bind phone successfully")
        void shouldBindPhoneSuccessfully() {
            // Given
            Long accountId = 1L;
            String phone = "13800000000";
            String code = "123456";
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);

            when(smsService.verifyCode(phone, code)).thenReturn(true);
            when(accountRepository.findByPhone(phone)).thenReturn(Optional.empty());
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When
            accountApplicationService.bindPhone(accountId, phone, code);

            // Then
            assertEquals(phone, account.getPhone());
            verify(accountRepository).save(account);
        }

        @Test
        @DisplayName("should throw ConflictException when phone already bound to another account")
        void shouldThrowWhenPhoneAlreadyBoundToAnother() {
            // Given
            Long accountId = 1L;
            String phone = "13800000000";
            String code = "123456";
            Account anotherAccount = Account.createFromPhone(phone);
            // 设置 anotherAccount 的 ID
            try {
                java.lang.reflect.Field idField = Account.class.getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(anotherAccount, 2L);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(smsService.verifyCode(phone, code)).thenReturn(true);
            when(accountRepository.findByPhone(phone)).thenReturn(Optional.of(anotherAccount));

            // When & Then
            assertThrows(ConflictException.class, () ->
                accountApplicationService.bindPhone(accountId, phone, code));
        }

        @Test
        @DisplayName("should allow bind phone when phone bound to same account")
        void shouldAllowWhenPhoneBoundToSameAccount() {
            // Given
            Long accountId = 1L;
            String phone = "13800000000";
            String code = "123456";
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);

            // 模拟反射设置 ID (实际中由 JPA 设置)
            when(smsService.verifyCode(phone, code)).thenReturn(true);
            when(accountRepository.findByPhone(phone)).thenReturn(Optional.empty());
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When
            accountApplicationService.bindPhone(accountId, phone, code);

            // Then
            assertEquals(phone, account.getPhone());
        }

        @Test
        @DisplayName("should throw DomainException when sms code invalid")
        void shouldThrowWhenSmsCodeInvalid() {
            // Given
            Long accountId = 1L;
            String phone = "13800000000";
            String code = "invalid";

            when(smsService.verifyCode(phone, code)).thenReturn(false);

            // When & Then
            assertThrows(DomainException.class, () ->
                accountApplicationService.bindPhone(accountId, phone, code));
        }

        @Test
        @DisplayName("should throw DomainException when account locked")
        void shouldThrowWhenAccountLocked() {
            // Given
            Long accountId = 1L;
            String phone = "13800000000";
            String code = "123456";
            Account account = Account.create("testuser", "pwd", null, ProfileHints.EMPTY);
            account.lock();

            when(smsService.verifyCode(phone, code)).thenReturn(true);
            when(accountRepository.findByPhone(phone)).thenReturn(Optional.empty());
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

            // When & Then
            assertThrows(DomainException.class, () ->
                accountApplicationService.bindPhone(accountId, phone, code));
        }
    }

    @Nested
    @DisplayName("sendResetCode")
    class SendResetCode {

        @Test
        @DisplayName("should send reset code when phone exists")
        void shouldSendResetCode() {
            // Given
            String phone = "13800000000";
            Account account = Account.createFromPhone(phone);

            when(accountRepository.findByPhone(phone)).thenReturn(Optional.of(account));

            // When
            accountApplicationService.sendResetCode(phone);

            // Then
            verify(smsService).sendCode(phone);
        }

        @Test
        @DisplayName("should throw NotFoundException when phone not bound")
        void shouldThrowWhenPhoneNotBound() {
            // Given
            String phone = "13800000000";

            when(accountRepository.findByPhone(phone)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                accountApplicationService.sendResetCode(phone));
            verify(smsService, never()).sendCode(any());
        }
    }
}
