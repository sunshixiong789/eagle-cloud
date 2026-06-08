package com.eagle.system.base.domain.model;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.system.base.domain.model.enums.Gender;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final Long ACCOUNT_ID = 100L;
    private static final String USERNAME = "alice";
    private static final String EMAIL = "alice@example.com";
    private static final UserProfile PROFILE =
            new UserProfile("https://avatar.example/a.png", "Alice", "Alice Real", Gender.FEMALE, "bio");

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("应创建用户")
        void shouldCreateUser() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);

            assertEquals(ACCOUNT_ID, user.getAccountId());
            assertEquals(USERNAME, user.getUsername());
            assertEquals(EMAIL, user.getEmail());
            assertEquals(PROFILE, user.getProfile());
            assertNotNull(user.getRoleIds());
            assertTrue(user.getRoleIds().isEmpty());
        }

        @Test
        @DisplayName("账号IDnull时应抛出")
        void shouldThrowWhenAccountIdNull() {
            AppException ex = assertThrows(DomainException.class,
                    () -> User.create(null, USERNAME, EMAIL, PROFILE));
            assertEquals(UserErrorCode.USERNAME_REQUIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("应允许null邮箱并资料")
        void shouldAllowNullEmailAndProfile() {
            User user = User.create(ACCOUNT_ID, USERNAME, null, null);
            assertNull(user.getEmail());
            assertNull(user.getProfile());
        }
    }

    @Nested
    @DisplayName("createForAccount")
    class CreateForAccount {

        @Test
        @DisplayName("应创建针对账号")
        void shouldCreateForAccount() {
            User user = User.createForAccount(ACCOUNT_ID, USERNAME, "13800138000", PROFILE);
            assertEquals(ACCOUNT_ID, user.getAccountId());
            assertEquals(USERNAME, user.getUsername());
            assertNull(user.getEmail());
            assertEquals(PROFILE, user.getProfile());
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("应替换资料")
        void shouldReplaceProfile() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            UserProfile newProfile = new UserProfile("https://new.png", "newnick", "new name", Gender.MALE, "new bio");
            user.updateProfile(newProfile);
            assertEquals(newProfile, user.getProfile());
        }
    }

    @Nested
    @DisplayName("updateContact")
    class UpdateContact {

        @Test
        @DisplayName("应更新邮箱")
        void shouldUpdateEmail() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.updateContact("new@example.com");
            assertEquals("new@example.com", user.getEmail());
        }

        @Test
        @DisplayName("应KeepCurrent邮箱")
        void shouldKeepCurrentEmail() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.updateContact(null);
            assertEquals(EMAIL, user.getEmail());
        }
    }

    @Nested
    @DisplayName("assignRoles")
    class AssignRoles {

        @Test
        @DisplayName("应Assign角色UnderCap")
        void shouldAssignRolesUnderCap() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            Set<Long> roles = Set.of(1L, 2L, 3L);
            user.assignRoles(roles);
            assertEquals(roles, user.getRoleIds());
        }

        @Test
        @DisplayName("null时应清理角色")
        void shouldClearRolesWhenNull() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.assignRoles(Set.of(1L, 2L));
            user.assignRoles(null);
            assertTrue(user.getRoleIds().isEmpty());
        }

        @Test
        @DisplayName("超过10角色时应抛出")
        void shouldThrowWhenOver10Roles() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            Set<Long> elevenRoles = LongStream.rangeClosed(1, 11).boxed().collect(Collectors.toSet());
            AppException ex = assertThrows(DomainException.class, () -> user.assignRoles(elevenRoles));
            assertEquals(UserErrorCode.MAX_ROLES_EXCEEDED, ex.getErrorCode());
        }

        @Test
        @DisplayName("应Accept10角色")
        void shouldAccept10Roles() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            Set<Long> tenRoles = LongStream.rangeClosed(1, 10).boxed().collect(Collectors.toSet());
            user.assignRoles(tenRoles);
            assertEquals(10, user.getRoleIds().size());
        }
    }
}
