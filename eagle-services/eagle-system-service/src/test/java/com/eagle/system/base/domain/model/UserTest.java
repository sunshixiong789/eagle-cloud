package com.eagle.system.base.domain.model;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.system.base.domain.model.enums.Gender;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
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
        @DisplayName("should create user with profile and default empty role/post sets")
        void shouldCreateUser() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);

            assertEquals(ACCOUNT_ID, user.getAccountId());
            assertEquals(USERNAME, user.getUsername());
            assertEquals(EMAIL, user.getEmail());
            assertEquals(PROFILE, user.getProfile());
            assertNotNull(user.getRoleIds());
            assertTrue(user.getRoleIds().isEmpty());
            assertNotNull(user.getPostIds());
            assertTrue(user.getPostIds().isEmpty());
        }

        @Test
        @DisplayName("should throw when accountId is null")
        void shouldThrowWhenAccountIdNull() {
            AppException ex = assertThrows(DomainException.class,
                    () -> User.create(null, USERNAME, EMAIL, PROFILE));
            assertEquals(UserErrorCode.USERNAME_REQUIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should allow null email and profile")
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
        @DisplayName("should create user from account event without email")
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
        @DisplayName("should replace profile")
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
        @DisplayName("should update email when provided")
        void shouldUpdateEmail() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.updateContact("new@example.com");
            assertEquals("new@example.com", user.getEmail());
        }

        @Test
        @DisplayName("should keep current email when input is null")
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
        @DisplayName("should set roles within the 10-role cap")
        void shouldAssignRolesUnderCap() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            Set<Long> roles = Set.of(1L, 2L, 3L);
            user.assignRoles(roles);
            assertEquals(roles, user.getRoleIds());
        }

        @Test
        @DisplayName("should clear roles when input is null")
        void shouldClearRolesWhenNull() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.assignRoles(Set.of(1L, 2L));
            user.assignRoles(null);
            assertTrue(user.getRoleIds().isEmpty());
        }

        @Test
        @DisplayName("should throw when more than 10 roles assigned")
        void shouldThrowWhenOver10Roles() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            Set<Long> elevenRoles = LongStream.rangeClosed(1, 11).boxed().collect(Collectors.toSet());
            AppException ex = assertThrows(DomainException.class, () -> user.assignRoles(elevenRoles));
            assertEquals(UserErrorCode.MAX_ROLES_EXCEEDED, ex.getErrorCode());
        }

        @Test
        @DisplayName("should accept exactly 10 roles")
        void shouldAccept10Roles() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            Set<Long> tenRoles = LongStream.rangeClosed(1, 10).boxed().collect(Collectors.toSet());
            user.assignRoles(tenRoles);
            assertEquals(10, user.getRoleIds().size());
        }
    }

    @Nested
    @DisplayName("assignDept / assignPosts")
    class AssignDeptAndPosts {

        @Test
        @DisplayName("should set dept id")
        void shouldAssignDept() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.assignDept(42L);
            assertEquals(42L, user.getDeptId());
        }

        @Test
        @DisplayName("should reassign posts replacing previous set")
        void shouldReassignPosts() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.assignPosts(new HashSet<>(Set.of(1L, 2L)));
            user.assignPosts(new HashSet<>(Set.of(3L)));
            assertEquals(Set.of(3L), user.getPostIds());
        }

        @Test
        @DisplayName("should clear posts when null")
        void shouldClearPostsWhenNull() {
            User user = User.create(ACCOUNT_ID, USERNAME, EMAIL, PROFILE);
            user.assignPosts(new HashSet<>(Set.of(1L)));
            user.assignPosts(null);
            assertTrue(user.getPostIds().isEmpty());
        }
    }
}
