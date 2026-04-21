package com.eagle.auth.application.service;

import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.repository.AccountRepository;
import com.eagle.auth.domain.service.WechatWebService.WechatWebUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * WechatWebUserService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("微信 Web 用户应用服务")
@ExtendWith(MockitoExtension.class)
class WechatWebUserServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private WechatWebUserService wechatWebUserService;

    @Nested
    @DisplayName("findOrCreateWechatWebAccount")
    class FindOrCreateWechatWebAccount {

        @Test
        @DisplayName("should find and return account by unionid (PC channel)")
        void shouldFindByUnionidPc() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "pc_openid_123", "unionid_123", "Tom", "http://avatar.url", "pc");
            Account existingAccount = Account.createFromWechat("openid_old", "unionid_123");

            when(accountRepository.findByWechatBindingUnionid("unionid_123"))
                .thenReturn(Optional.of(existingAccount));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertEquals(existingAccount, result);
            assertNotNull(result.getWechatBinding());
            assertEquals("pc_openid_123", result.getWechatBinding().getWebOpenid());
            verify(accountRepository).save(existingAccount);
        }

        @Test
        @DisplayName("should find and return account by unionid (H5 channel)")
        void shouldFindByUnionidH5() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "mp_openid_123", "unionid_123", "Jerry", "http://avatar.url", "h5");
            Account existingAccount = Account.createFromWechat("openid_old", "unionid_123");

            when(accountRepository.findByWechatBindingUnionid("unionid_123"))
                .thenReturn(Optional.of(existingAccount));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertEquals(existingAccount, result);
            assertNotNull(result.getWechatBinding());
            assertEquals("mp_openid_123", result.getWechatBinding().getMpOpenid());
            verify(accountRepository).save(existingAccount);
        }

        @Test
        @DisplayName("should find account by web openid when unionid not found")
        void shouldFindByWebOpenid() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "pc_openid_123", null, "Tom", "http://avatar.url", "pc");
            Account existingAccount = Account.createFromWechatWeb("pc_openid_123", null, "Tom", null);

            when(accountRepository.findByWechatBindingWebOpenid("pc_openid_123"))
                .thenReturn(Optional.of(existingAccount));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertEquals(existingAccount, result);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should find account by mp openid when unionid not found")
        void shouldFindByMpOpenid() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "mp_openid_123", null, "Jerry", "http://avatar.url", "h5");
            Account existingAccount = Account.createFromWechatH5("mp_openid_123", null, "Jerry", null);

            when(accountRepository.findByWechatBindingMpOpenid("mp_openid_123"))
                .thenReturn(Optional.of(existingAccount));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertEquals(existingAccount, result);
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new account for PC channel when not found")
        void shouldCreateNewAccountForPc() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "pc_openid_new", "unionid_new", "Tom", "http://avatar.url", "pc");

            when(accountRepository.findByWechatBindingUnionid("unionid_new"))
                .thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingWebOpenid("pc_openid_new"))
                .thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertNotNull(result);
            assertEquals("wxweb_pc_openid_new", result.getUsername());
            assertNotNull(result.getWechatBinding());
            assertEquals("pc_openid_new", result.getWechatBinding().getWebOpenid());
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should create new account for H5 channel when not found")
        void shouldCreateNewAccountForH5() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "mp_openid_new", "unionid_new", "Jerry", "http://avatar.url", "h5");

            when(accountRepository.findByWechatBindingUnionid("unionid_new"))
                .thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingMpOpenid("mp_openid_new"))
                .thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertNotNull(result);
            assertEquals("wxmp_mp_openid_new", result.getUsername());
            assertNotNull(result.getWechatBinding());
            assertEquals("mp_openid_new", result.getWechatBinding().getMpOpenid());
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should update unionid when found by openid but unionid is new")
        void shouldUpdateUnionidWhenFoundByOpenid() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "pc_openid_123", "unionid_new", "Tom", "http://avatar.url", "pc");
            Account existingAccount = Account.createFromWechatWeb("pc_openid_123", null, "Tom", null);

            when(accountRepository.findByWechatBindingUnionid("unionid_new"))
                .thenReturn(Optional.empty());
            when(accountRepository.findByWechatBindingWebOpenid("pc_openid_123"))
                .thenReturn(Optional.of(existingAccount));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertEquals(existingAccount, result);
            assertEquals("unionid_new", result.getWechatBinding().getUnionid());
            verify(accountRepository).save(existingAccount);
        }

        @Test
        @DisplayName("should handle null unionid gracefully")
        void shouldHandleNullUnionid() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "pc_openid_123", null, "Tom", "http://avatar.url", "pc");

            when(accountRepository.findByWechatBindingWebOpenid("pc_openid_123"))
                .thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertNotNull(result);
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should handle blank unionid gracefully")
        void shouldHandleBlankUnionid() {
            // Given
            WechatWebUserInfo info = new WechatWebUserInfo(
                "pc_openid_123", "  ", "Tom", "http://avatar.url", "pc");

            when(accountRepository.findByWechatBindingWebOpenid("pc_openid_123"))
                .thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Account result = wechatWebUserService.findOrCreateWechatWebAccount(info);

            // Then
            assertNotNull(result);
            verify(accountRepository).save(any(Account.class));
        }
    }
}
