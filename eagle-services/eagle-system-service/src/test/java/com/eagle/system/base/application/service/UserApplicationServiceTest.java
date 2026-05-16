package com.eagle.system.base.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.enums.Gender;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.domain.service.RoleValidationService;
import com.eagle.system.base.interfaces.dto.request.UpdateUserRequest;
import com.eagle.system.base.interfaces.dto.response.AssignedRoleResponse;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long ACCOUNT_ID = 200L;

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock RoleValidationService roleValidationService;
    @Mock RoleRepository roleRepository;
    @InjectMocks UserApplicationService service;

    private User sampleUser() {
        UserProfile profile = new UserProfile("https://a.png", "Alice", "Alice Real", Gender.FEMALE, "bio");
        return User.create(ACCOUNT_ID, "alice", "alice@example.com", profile);
    }

    @Nested
    @DisplayName("updateUser")
    class Update {
        @Test
        @DisplayName("should update profile fields and email")
        void shouldUpdate() {
            User user = sampleUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().build());

            UpdateUserRequest req = new UpdateUserRequest();
            req.setNickname("NewNick");
            req.setEmail("new@example.com");

            service.updateUser(USER_ID, req);

            assertEquals("NewNick", user.getProfile().getNickname());
            assertEquals("new@example.com", user.getEmail());
        }

        @Test
        @DisplayName("should throw NotFound when user missing")
        void shouldThrowWhenMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.updateUser(USER_ID, new UpdateUserRequest()));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("should preserve existing profile when request fields are null")
        void shouldPreserveProfileWhenNoChanges() {
            User user = sampleUser();
            String originalNick = user.getProfile().getNickname();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().build());

            service.updateUser(USER_ID, new UpdateUserRequest());

            assertEquals(originalNick, user.getProfile().getNickname());
        }
    }

    @Nested
    @DisplayName("assignRoles")
    class AssignRoles {
        @Test
        @DisplayName("should validate and assign roles via aggregate")
        void shouldAssign() {
            User user = sampleUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            Set<Long> roleIds = Set.of(1L, 2L);

            service.assignRoles(USER_ID, roleIds);

            verify(roleValidationService).validateRoles(roleIds);
            assertEquals(roleIds, user.getRoleIds());
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("getUserRoles")
    class GetUserRoles {
        @Test
        @DisplayName("should return empty list when user has no roles")
        void shouldReturnEmpty() {
            User user = sampleUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            List<AssignedRoleResponse> roles = service.getUserRoles(USER_ID);
            assertTrue(roles.isEmpty());
        }

        @Test
        @DisplayName("should map roles to assigned role responses")
        void shouldMapRoles() {
            User user = sampleUser();
            user.assignRoles(Set.of(1L, 2L));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            Role r1 = Role.create("Admin", "admin", null, 1);
            Role r2 = Role.create("Op", "op", null, 2);
            when(roleRepository.findAllById(any())).thenReturn(List.of(r1, r2));

            List<AssignedRoleResponse> roles = service.getUserRoles(USER_ID);

            assertEquals(2, roles.size());
            assertEquals("ENABLE", roles.get(0).getStatus());
        }
    }
}
