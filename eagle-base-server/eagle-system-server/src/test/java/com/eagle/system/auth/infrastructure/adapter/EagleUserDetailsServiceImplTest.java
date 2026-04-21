package com.eagle.auth.infrastructure.adapter;

import com.eagle.auth.domain.model.Account;
import com.eagle.auth.domain.model.valueobject.ProfileHints;
import com.eagle.auth.domain.port.AuthorizationInfo;
import com.eagle.auth.domain.port.AuthorizationPort;
import com.eagle.auth.domain.repository.AccountRepository;
import com.eagle.common.dto.EagleUser;
import com.eagle.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * EagleUserDetailsServiceImpl 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("UserDetailsService 实现")
@ExtendWith(MockitoExtension.class)
class EagleUserDetailsServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthorizationPort authorizationPort;

    @InjectMocks
    private EagleUserDetailsServiceImpl eagleUserDetailsService;

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should load user with full authorization info")
        void shouldLoadUserWithFullAuthorizationInfo() {
            // Given
            String username = "admin";
            Account account = Account.create(username, "encoded_pwd", "13800000000", ProfileHints.EMPTY);
            setAccountId(account, 1L);

            AuthorizationInfo authInfo = new AuthorizationInfo(
                "管理员", 1L, "技术部", Set.of("ROLE_admin", "ROLE_user")
            );

            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
            when(authorizationPort.findAuthorizationInfo(1L)).thenReturn(Optional.of(authInfo));

            // When
            UserDetails userDetails = eagleUserDetailsService.loadUserByUsername(username);

            // Then
            assertNotNull(userDetails);
            assertInstanceOf(EagleUser.class, userDetails);
            EagleUser eagleUser = (EagleUser) userDetails;
            assertEquals(username, eagleUser.getUsername());
            assertEquals("encoded_pwd", eagleUser.getPassword());
            assertEquals("管理员", eagleUser.getName());
            assertEquals(1L, eagleUser.getDeptId());
            assertEquals("技术部", eagleUser.getDeptName());
            assertTrue(eagleUser.isEnabled());
            assertTrue(eagleUser.isAccountNonLocked());
            assertEquals(2, eagleUser.getAuthorities().size());
        }

        @Test
        @DisplayName("should load user with empty authorization info")
        void shouldLoadUserWithEmptyAuthorizationInfo() {
            // Given
            String username = "newuser";
            Account account = Account.create(username, "encoded_pwd", null, ProfileHints.EMPTY);
            setAccountId(account, 2L);

            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
            when(authorizationPort.findAuthorizationInfo(2L)).thenReturn(Optional.empty());

            // When
            UserDetails userDetails = eagleUserDetailsService.loadUserByUsername(username);

            // Then
            assertNotNull(userDetails);
            EagleUser eagleUser = (EagleUser) userDetails;
            assertEquals(username, eagleUser.getName());
            assertNull(eagleUser.getDeptId());
            assertNull(eagleUser.getDeptName());
            assertTrue(eagleUser.getAuthorities().isEmpty());
        }

        @Test
        @DisplayName("should load locked user as disabled")
        void shouldLoadLockedUserAsDisabled() {
            // Given
            String username = "lockeduser";
            Account account = Account.create(username, "encoded_pwd", null, ProfileHints.EMPTY);
            setAccountId(account, 3L);
            account.lock();

            when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));
            when(authorizationPort.findAuthorizationInfo(3L)).thenReturn(Optional.empty());

            // When
            UserDetails userDetails = eagleUserDetailsService.loadUserByUsername(username);

            // Then
            assertNotNull(userDetails);
            assertFalse(userDetails.isEnabled());
            assertFalse(userDetails.isAccountNonLocked());
        }

        @Test
        @DisplayName("should throw NotFoundException when account not found")
        void shouldThrowWhenAccountNotFound() {
            // Given
            String username = "nonexistent";
            when(accountRepository.findByUsername(username)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                eagleUserDetailsService.loadUserByUsername(username));
        }
    }

    private void setAccountId(Account account, Long id) {
        try {
            java.lang.reflect.Field idField = Account.class.getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(account, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
