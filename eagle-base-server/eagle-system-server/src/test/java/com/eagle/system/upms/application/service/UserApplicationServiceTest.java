package com.eagle.system.application.service;

import com.eagle.common.exception.NotFoundException;
import com.eagle.system.application.mapper.UserMapper;
import com.eagle.system.domain.model.User;
import com.eagle.system.domain.model.valueobject.UserProfile;
import com.eagle.system.domain.repository.UserRepository;
import com.eagle.system.domain.repository.UserSummary;
import com.eagle.system.domain.service.RoleValidationService;
import com.eagle.system.web.dto.request.UpdateUserRequest;
import com.eagle.system.web.dto.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("用户应用服务")
@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleValidationService roleValidationService;

    @InjectMocks
    private UserApplicationService userApplicationService;

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("should update user profile successfully")
        void shouldUpdateUserProfileSuccessfully() {
            // Given
            Long userId = 1L;
            UpdateUserRequest request = new UpdateUserRequest();
            request.setName("New Name");
            request.setNickname("New Nick");
            request.setAvatar("new_avatar.jpg");

            User existingUser = User.create(1L, "testuser", "test@example.com", null);
            UserResponse response = new UserResponse();

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toResponse(any(User.class))).thenReturn(response);

            // When
            UserResponse result = userApplicationService.updateUser(userId, request);

            // Then
            assertNotNull(result);
            assertNotNull(existingUser.getProfile());
            assertEquals("New Name", existingUser.getProfile().getName());
            assertEquals("New Nick", existingUser.getProfile().getNickname());
            assertEquals("new_avatar.jpg", existingUser.getProfile().getAvatar());
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("should update email successfully")
        void shouldUpdateEmailSuccessfully() {
            // Given
            Long userId = 1L;
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("new@example.com");

            User existingUser = User.create(1L, "testuser", "old@example.com", null);
            UserResponse response = new UserResponse();

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toResponse(any(User.class))).thenReturn(response);

            // When
            UserResponse result = userApplicationService.updateUser(userId, request);

            // Then
            assertNotNull(result);
            assertEquals("new@example.com", existingUser.getEmail());
            verify(userRepository).save(existingUser);
        }

        @Test
        @DisplayName("should throw NotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            Long userId = 999L;
            UpdateUserRequest request = new UpdateUserRequest();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                userApplicationService.updateUser(userId, request));
        }

        @Test
        @DisplayName("should not update profile when all fields null")
        void shouldNotUpdateProfileWhenAllFieldsNull() {
            // Given
            Long userId = 1L;
            UpdateUserRequest request = new UpdateUserRequest();
            request.setEmail("new@example.com");

            User existingUser = User.create(1L, "testuser", "old@example.com", null);
            UserResponse response = new UserResponse();

            when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userMapper.toResponse(any(User.class))).thenReturn(response);

            // When
            UserResponse result = userApplicationService.updateUser(userId, request);

            // Then
            assertNotNull(result);
            assertNull(existingUser.getProfile());
            assertEquals("new@example.com", existingUser.getEmail());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user response when user exists")
        void shouldReturnUserResponse() {
            // Given
            Long userId = 1L;
            User user = User.create(1L, "testuser", "test@example.com", null);
            UserResponse response = new UserResponse();

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(response);

            // When
            UserResponse result = userApplicationService.getUserById(userId);

            // Then
            assertNotNull(result);
            verify(userMapper).toResponse(user);
        }

        @Test
        @DisplayName("should throw NotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            Long userId = 999L;

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                userApplicationService.getUserById(userId));
        }
    }

    @Nested
    @DisplayName("queryUsers")
    class QueryUsers {

        @Test
        @DisplayName("should return paginated users")
        void shouldReturnPaginatedUsers() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            User user1 = User.create(1L, "user1", "user1@example.com", null);
            User user2 = User.create(2L, "user2", "user2@example.com", null);
            Page<User> userPage = new PageImpl<>(List.of(user1, user2));
            UserResponse response1 = new UserResponse();
            UserResponse response2 = new UserResponse();

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toResponse(user1)).thenReturn(response1);
            when(userMapper.toResponse(user2)).thenReturn(response2);

            // When
            Page<UserResponse> result = userApplicationService.queryUsers(pageable);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("queryUserSummaries")
    class QueryUserSummaries {

        @Test
        @DisplayName("should return paginated user summaries")
        void shouldReturnPaginatedUserSummaries() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            UserSummary summary1 = mock(UserSummary.class);
            UserSummary summary2 = mock(UserSummary.class);
            Page<UserSummary> summaryPage = new PageImpl<>(List.of(summary1, summary2));

            when(userRepository.findUserSummaries(pageable)).thenReturn(summaryPage);

            // When
            Page<UserSummary> result = userApplicationService.queryUserSummaries(pageable);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("assignRoles")
    class AssignRoles {

        @Test
        @DisplayName("should assign roles successfully")
        void shouldAssignRolesSuccessfully() {
            // Given
            Long userId = 1L;
            Set<Long> roleIds = Set.of(1L, 2L, 3L);
            User user = User.create(1L, "testuser", null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(roleValidationService).validateRoles(roleIds);

            // When
            userApplicationService.assignRoles(userId, roleIds);

            // Then
            assertEquals(3, user.getRoleIds().size());
            verify(roleValidationService).validateRoles(roleIds);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw NotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            Long userId = 999L;
            Set<Long> roleIds = Set.of(1L);

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                userApplicationService.assignRoles(userId, roleIds));
            verify(roleValidationService, never()).validateRoles(any());
        }

        @Test
        @DisplayName("should validate roles before assigning")
        void shouldValidateRolesBeforeAssigning() {
            // Given
            Long userId = 1L;
            Set<Long> roleIds = Set.of(1L, 2L);
            User user = User.create(1L, "testuser", null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userApplicationService.assignRoles(userId, roleIds);

            // Then
            verify(roleValidationService).validateRoles(roleIds);
        }
    }

    @Nested
    @DisplayName("assignDept")
    class AssignDept {

        @Test
        @DisplayName("should assign department successfully")
        void shouldAssignDeptSuccessfully() {
            // Given
            Long userId = 1L;
            Long deptId = 10L;
            User user = User.create(1L, "testuser", null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userApplicationService.assignDept(userId, deptId);

            // Then
            assertEquals(deptId, user.getDeptId());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw NotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            Long userId = 999L;
            Long deptId = 10L;

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                userApplicationService.assignDept(userId, deptId));
        }
    }

    @Nested
    @DisplayName("assignPosts")
    class AssignPosts {

        @Test
        @DisplayName("should assign posts successfully")
        void shouldAssignPostsSuccessfully() {
            // Given
            Long userId = 1L;
            Set<Long> postIds = Set.of(1L, 2L, 3L);
            User user = User.create(1L, "testuser", null, null);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            userApplicationService.assignPosts(userId, postIds);

            // Then
            assertEquals(3, user.getPostIds().size());
            assertTrue(user.getPostIds().containsAll(postIds));
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("should throw NotFoundException when user not found")
        void shouldThrowWhenUserNotFound() {
            // Given
            Long userId = 999L;
            Set<Long> postIds = Set.of(1L);

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                userApplicationService.assignPosts(userId, postIds));
        }
    }
}
