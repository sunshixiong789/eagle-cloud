package com.eagle.system.domain.model;

import com.eagle.common.exception.DomainException;
import com.eagle.system.domain.model.valueobject.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User 聚合根单元测试
 *
 * @author sunshixiong
 */
@DisplayName("User 聚合根")
class UserTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create user when accountId is valid")
        void shouldCreateUserWhenAccountIdIsValid() {
            // Given
            Long accountId = 1L;
            String username = "testuser";
            String email = "test@example.com";
            UserProfile profile = new UserProfile(null, "Nick", "Name", null, null);

            // When
            User user = User.create(accountId, username, email, profile);

            // Then
            assertNotNull(user);
            assertEquals(accountId, user.getAccountId());
            assertEquals(username, user.getUsername());
            assertEquals(email, user.getEmail());
            assertEquals(profile, user.getProfile());
            assertTrue(user.getRoleIds().isEmpty());
            assertNull(user.getDeptId());
        }

        @Test
        @DisplayName("should create user with null profile")
        void shouldCreateUserWithNullProfile() {
            // Given
            Long accountId = 1L;
            String username = "testuser";

            // When
            User user = User.create(accountId, username, null, null);

            // Then
            assertNotNull(user);
            assertNull(user.getProfile());
        }

        @Test
        @DisplayName("should throw DomainException when accountId is null")
        void shouldThrowWhenAccountIdIsNull() {
            // When & Then
            assertThrows(DomainException.class, () ->
                User.create(null, "testuser", null, null));
        }
    }

    @Nested
    @DisplayName("createForAccount")
    class CreateForAccount {

        @Test
        @DisplayName("should create user for account event")
        void shouldCreateUserForAccountEvent() {
            // Given
            Long accountId = 1L;
            String username = "testuser";
            String phone = "13800000000";
            UserProfile profile = new UserProfile(null, "Nick", "Name", null, null);

            // When
            User user = User.createForAccount(accountId, username, phone, profile);

            // Then
            assertNotNull(user);
            assertEquals(accountId, user.getAccountId());
            assertEquals(username, user.getUsername());
            assertEquals(profile, user.getProfile());
        }

        @Test
        @DisplayName("should create user with null profile")
        void shouldCreateUserWithNullProfile() {
            // Given
            Long accountId = 1L;
            String username = "testuser";

            // When
            User user = User.createForAccount(accountId, username, null, null);

            // Then
            assertNotNull(user);
            assertNull(user.getProfile());
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @Test
        @DisplayName("should update profile successfully")
        void shouldUpdateProfileSuccessfully() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            UserProfile newProfile = new UserProfile("avatar", "NewNick", "NewName", null, null);

            // When
            user.updateProfile(newProfile);

            // Then
            assertEquals(newProfile, user.getProfile());
        }

        @Test
        @DisplayName("should update profile with null")
        void shouldUpdateProfileWithNull() {
            // Given
            User user = User.create(1L, "testuser", null, 
                new UserProfile("avatar", "Nick", "Name", null, null));

            // When
            user.updateProfile(null);

            // Then
            assertNull(user.getProfile());
        }
    }

    @Nested
    @DisplayName("updateContact")
    class UpdateContact {

        @Test
        @DisplayName("should update email when provided")
        void shouldUpdateEmailWhenProvided() {
            // Given
            User user = User.create(1L, "testuser", "old@example.com", null);

            // When
            user.updateContact("new@example.com");

            // Then
            assertEquals("new@example.com", user.getEmail());
        }

        @Test
        @DisplayName("should not update email when null")
        void shouldNotUpdateEmailWhenNull() {
            // Given
            User user = User.create(1L, "testuser", "old@example.com", null);

            // When
            user.updateContact(null);

            // Then
            assertEquals("old@example.com", user.getEmail());
        }
    }

    @Nested
    @DisplayName("assignRoles")
    class AssignRoles {

        @Test
        @DisplayName("should assign roles successfully")
        void shouldAssignRolesSuccessfully() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            Set<Long> roleIds = Set.of(1L, 2L, 3L);

            // When
            user.assignRoles(roleIds);

            // Then
            assertEquals(3, user.getRoleIds().size());
            assertTrue(user.getRoleIds().containsAll(roleIds));
        }

        @Test
        @DisplayName("should clear roles when null")
        void shouldClearRolesWhenNull() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignRoles(Set.of(1L, 2L));

            // When
            user.assignRoles(null);

            // Then
            assertTrue(user.getRoleIds().isEmpty());
        }

        @Test
        @DisplayName("should clear roles when empty set")
        void shouldClearRolesWhenEmptySet() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignRoles(Set.of(1L, 2L));

            // When
            user.assignRoles(Set.of());

            // Then
            assertTrue(user.getRoleIds().isEmpty());
        }

        @Test
        @DisplayName("should throw DomainException when roles exceed 10")
        void shouldThrowWhenRolesExceed10() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            Set<Long> roleIds = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);

            // When & Then
            assertThrows(DomainException.class, () -> user.assignRoles(roleIds));
        }

        @Test
        @DisplayName("should allow exactly 10 roles")
        void shouldAllowExactly10Roles() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            Set<Long> roleIds = Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

            // When
            user.assignRoles(roleIds);

            // Then
            assertEquals(10, user.getRoleIds().size());
        }

        @Test
        @DisplayName("should replace existing roles")
        void shouldReplaceExistingRoles() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignRoles(Set.of(1L, 2L));

            // When
            user.assignRoles(Set.of(3L, 4L));

            // Then
            assertEquals(2, user.getRoleIds().size());
            assertTrue(user.getRoleIds().contains(3L));
            assertTrue(user.getRoleIds().contains(4L));
            assertFalse(user.getRoleIds().contains(1L));
        }
    }

    @Nested
    @DisplayName("assignDept")
    class AssignDept {

        @Test
        @DisplayName("should assign department successfully")
        void shouldAssignDeptSuccessfully() {
            // Given
            User user = User.create(1L, "testuser", null, null);

            // When
            user.assignDept(10L);

            // Then
            assertEquals(10L, user.getDeptId());
        }

        @Test
        @DisplayName("should update existing department")
        void shouldUpdateExistingDept() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignDept(10L);

            // When
            user.assignDept(20L);

            // Then
            assertEquals(20L, user.getDeptId());
        }

        @Test
        @DisplayName("should clear department when null")
        void shouldClearDeptWhenNull() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignDept(10L);

            // When
            user.assignDept(null);

            // Then
            assertNull(user.getDeptId());
        }
    }

    @Nested
    @DisplayName("assignPosts")
    class AssignPosts {

        @Test
        @DisplayName("should assign posts successfully")
        void shouldAssignPostsSuccessfully() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            Set<Long> postIds = Set.of(1L, 2L, 3L);

            // When
            user.assignPosts(postIds);

            // Then
            assertEquals(3, user.getPostIds().size());
            assertTrue(user.getPostIds().containsAll(postIds));
        }

        @Test
        @DisplayName("should clear posts when null")
        void shouldClearPostsWhenNull() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignPosts(Set.of(1L, 2L));

            // When
            user.assignPosts(null);

            // Then
            assertTrue(user.getPostIds().isEmpty());
        }

        @Test
        @DisplayName("should clear posts when empty set")
        void shouldClearPostsWhenEmptySet() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignPosts(Set.of(1L, 2L));

            // When
            user.assignPosts(Set.of());

            // Then
            assertTrue(user.getPostIds().isEmpty());
        }

        @Test
        @DisplayName("should replace existing posts")
        void shouldReplaceExistingPosts() {
            // Given
            User user = User.create(1L, "testuser", null, null);
            user.assignPosts(Set.of(1L, 2L));

            // When
            user.assignPosts(Set.of(3L, 4L));

            // Then
            assertEquals(2, user.getPostIds().size());
            assertTrue(user.getPostIds().contains(3L));
            assertTrue(user.getPostIds().contains(4L));
            assertFalse(user.getPostIds().contains(1L));
        }
    }
}
