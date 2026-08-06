package com.eagle.system.base.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.base.application.mapper.UserMapper;
import com.eagle.system.base.domain.model.Role;
import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.model.enums.Gender;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.model.enums.LogType;
import com.eagle.system.base.domain.model.enums.UserErrorCode;
import com.eagle.system.base.domain.model.valueobject.UserProfile;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.domain.repository.RoleRepository;
import com.eagle.system.base.domain.repository.UserRepository;
import com.eagle.system.base.domain.service.RoleValidationService;
import com.eagle.system.base.infrastructure.config.AdminProperties;
import com.eagle.system.base.interfaces.dto.request.UpdateProfileRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateUserRequest;
import com.eagle.system.base.interfaces.dto.request.UserQueryRequest;
import com.eagle.system.base.interfaces.dto.response.AssignedRoleResponse;
import com.eagle.system.base.infrastructure.remote.AuthClientFacade;
import com.eagle.system.base.infrastructure.remote.dto.AccountBlacklistSnapshot;
import com.eagle.system.base.infrastructure.remote.dto.AccountSnapshot;
import com.eagle.system.base.interfaces.dto.response.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
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

    @Mock
    UserRepository userRepository;
    @Mock
    UserMapper userMapper;
    @Mock
    RoleValidationService roleValidationService;
    @Mock
    RoleRepository roleRepository;
    @Mock
    LogRepository logRepository;
    @Mock
    AuthClientFacade authClientFacade;
    @Mock
    AdminProperties adminProperties;
    @InjectMocks
    UserApplicationService service;

    private User sampleUser() {
        UserProfile profile = new UserProfile("https://a.png", "Alice", "Alice Real", Gender.FEMALE, "bio");
        return User.create(ACCOUNT_ID, "alice", "alice@example.com", profile);
    }

    /**
     * UserMapper 的返回值占位：只填用例关心的字段，其余为 null / false。
     * 富化字段（角色、在线态、黑名单）由被测方法自己回填，不在这里给。
     */
    private static UserResponse mappedResponse(Long id, Long accountId, String username) {
        return new UserResponse(id, accountId, username, null, null, null, null, null,
                null, null, false, null, false, null, null);
    }

    @Nested
    @DisplayName("updateUser")
    class Update {
        @Test
        @DisplayName("应更新")
        void shouldUpdate() {
            User user = sampleUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(mappedResponse(null, null, null));

            UpdateUserRequest req = new UpdateUserRequest(null, null, "NewNick", null, "new@example.com");

            service.updateUser(USER_ID, req);

            assertEquals("NewNick", user.getProfile().getNickname());
            assertEquals("new@example.com", user.getEmail());
        }

        @Test
        @DisplayName("缺失时应抛出")
        void shouldThrowWhenMissing() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.updateUser(USER_ID, new UpdateUserRequest(null, null, null, null, null)));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("无Changes时应保留资料")
        void shouldPreserveProfileWhenNoChanges() {
            User user = sampleUser();
            String originalNick = user.getProfile().getNickname();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(mappedResponse(null, null, null));

            service.updateUser(USER_ID, new UpdateUserRequest(null, null, null, null, null));

            assertEquals(originalNick, user.getProfile().getNickname());
        }
    }

    @Nested
    @DisplayName("queryUsers")
    class QueryUsers {
        @Test
        @DisplayName("应Enrich用户列表")
        void shouldEnrichUserList() {
            User user = sampleUser();
            user.assignRoles(Set.of(1L));
            UserResponse base = mappedResponse(USER_ID, ACCOUNT_ID, "alice");
            Role admin = Role.create("Admin", "admin", null, 1);
            LocalDateTime lastLoginAt = LocalDateTime.of(2026, 5, 20, 9, 30);

            when(adminProperties.getUsername()).thenReturn("admin");
            when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                    any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(user)));
            when(userMapper.toResponse(user)).thenReturn(base);
            when(roleRepository.findAllById(user.getRoleIds())).thenReturn(List.of(admin));
            when(logRepository.findLatestCreateTimeByUsernameAndLogTypeAndStatus(
                    "alice", LogType.LOGIN, LogStatus.SUCCESS)).thenReturn(Optional.of(lastLoginAt));
            when(authClientFacade.listJtisByAccount(ACCOUNT_ID)).thenReturn(List.of("jti-1"));
            when(authClientFacade.findBlacklistByAccountId(ACCOUNT_ID))
                    .thenReturn(ResponseEntity.ok(new AccountBlacklistSnapshot(300L, ACCOUNT_ID.toString())));

            Page<UserResponse> page = service.queryUsers(PageRequest.of(0, 10));

            UserResponse response = page.getContent().getFirst();
            assertEquals(ACCOUNT_ID, response.accountId());
            assertEquals(lastLoginAt, response.lastLoginAt());
            assertTrue(response.online());
            assertEquals("ONLINE", response.loginStatus());
            assertTrue(response.blacklisted());
            assertEquals(300L, response.blacklistId());
            assertEquals(1, response.roles().size());
            assertEquals("Admin", response.roles().getFirst().roleName());
        }

        @Test
        @DisplayName("应Exclude管理员从用户列表")
        void shouldExcludeAdminFromUserList() {
            when(adminProperties.getUsername()).thenReturn("admin");
            when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                    any(PageRequest.class))).thenReturn(Page.empty());

            service.queryUsers(PageRequest.of(0, 10));
            service.queryUsers(new UserQueryRequest(null, null, null, null, null), PageRequest.of(0, 10));

            verify(userRepository, org.mockito.Mockito.times(2))
                    .findAll(any(org.springframework.data.jpa.domain.Specification.class),
                            any(PageRequest.class));
        }

        @Test
        @DisplayName("无排序参数时应按最新创建时间倒序")
        void shouldApplyNewestFirstSortWhenUnsorted() {
            when(adminProperties.getUsername()).thenReturn("admin");
            when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                    any(PageRequest.class))).thenReturn(Page.empty());

            service.queryUsers(new UserQueryRequest(null, null, null, null, null), PageRequest.of(0, 10));

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(userRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                    pageableCaptor.capture());
            Sort sort = pageableCaptor.getValue().getSort();
            assertEquals(Sort.Direction.DESC, sort.getOrderFor("createTime").getDirection());
            assertEquals(Sort.Direction.DESC, sort.getOrderFor("id").getDirection());
        }

        @Test
        @DisplayName("应按当前页 accountId 批量回填手机号且只调用 auth 一次")
        void shouldEnrichPhonesWithSingleBatchCall() {
            User first = sampleUser();
            User second = User.create(201L, "bob", "bob@example.com", null);
            UserResponse firstResponse = mappedResponse(USER_ID, ACCOUNT_ID, null);
            UserResponse secondResponse = mappedResponse(101L, 201L, null);
            when(adminProperties.getUsername()).thenReturn("admin");
            when(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                    any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(first, second)));
            when(userMapper.toResponse(first)).thenReturn(firstResponse);
            when(userMapper.toResponse(second)).thenReturn(secondResponse);
            when(logRepository.findLatestCreateTimeByUsernameAndLogTypeAndStatus(
                    any(), any(), any())).thenReturn(Optional.empty());
            when(authClientFacade.listJtisByAccount(ACCOUNT_ID)).thenReturn(List.of());
            when(authClientFacade.listJtisByAccount(201L)).thenReturn(List.of());
            when(authClientFacade.findBlacklistByAccountId(ACCOUNT_ID))
                    .thenReturn(ResponseEntity.noContent().build());
            when(authClientFacade.findBlacklistByAccountId(201L))
                    .thenReturn(ResponseEntity.noContent().build());
            when(authClientFacade.findAccounts(Set.of(ACCOUNT_ID, 201L))).thenReturn(List.of(
                    new AccountSnapshot(ACCOUNT_ID, "alice", "17708080863"),
                    new AccountSnapshot(201L, "bob", "18800000008")));

            Page<UserResponse> page = service.queryUsers(PageRequest.of(0, 10));

            assertEquals("17708080863", page.getContent().get(0).phone());
            assertEquals("18800000008", page.getContent().get(1).phone());
            verify(authClientFacade).findAccounts(Set.of(ACCOUNT_ID, 201L));
        }
    }

    @Nested
    @DisplayName("assignRoles")
    class AssignRoles {
        @Test
        @DisplayName("应Assign")
        void shouldAssign() {
            User user = sampleUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            Set<Long> roleIds = Set.of(1L, 2L);

            service.assignRoles(USER_ID, roleIds);

            verify(roleValidationService).validateRoles(roleIds);
            assertEquals(roleIds, user.getRoleIds());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("应拒绝Assigning角色到管理员")
        void shouldRejectAssigningRolesToAdmin() {
            User admin = User.create(ACCOUNT_ID, "admin", "admin@example.com", null);
            when(adminProperties.getUsername()).thenReturn("admin");
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(admin));

            AppException ex = assertThrows(AppException.class,
                    () -> service.assignRoles(USER_ID, Set.of(1L)));

            assertEquals(UserErrorCode.ADMIN_USER_PROTECTED, ex.getErrorCode());
            verify(roleValidationService, org.mockito.Mockito.never()).validateRoles(any());
            verify(userRepository, org.mockito.Mockito.never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCurrentUserByAccountId")
    class GetCurrentUser {
        @Test
        @DisplayName("应按 accountId 返回当前用户")
        void shouldReturnByAccountId() {
            User user = sampleUser();
            UserResponse mapped = mappedResponse(null, ACCOUNT_ID, "alice");
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(user));
            when(userMapper.toResponse(user)).thenReturn(mapped);

            UserResponse res = service.getCurrentUserByAccountId(ACCOUNT_ID);

            assertEquals(ACCOUNT_ID, res.accountId());
        }

        @Test
        @DisplayName("缺失时应抛出 USER_NOT_FOUND")
        void shouldThrowWhenMissing() {
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.getCurrentUserByAccountId(ACCOUNT_ID));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("updateCurrentUserByAccountId")
    class UpdateCurrentUser {
        @Test
        @DisplayName("应按 accountId 更新当前用户档案")
        void shouldUpdateByAccountId() {
            User user = sampleUser();
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toResponse(user)).thenReturn(mappedResponse(null, null, null));

            UpdateProfileRequest req = new UpdateProfileRequest(null, "NewNick", null, "new@example.com");

            service.updateCurrentUserByAccountId(ACCOUNT_ID, req);

            assertEquals("NewNick", user.getProfile().getNickname());
            assertEquals("new@example.com", user.getEmail());
        }

        @Test
        @DisplayName("缺失时应抛出 USER_NOT_FOUND")
        void shouldThrowWhenMissing() {
            when(userRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class,
                    () -> service.updateCurrentUserByAccountId(ACCOUNT_ID, new UpdateProfileRequest(null, null, null, null)));
            assertEquals(UserErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getUserRoles")
    class GetUserRoles {
        @Test
        @DisplayName("应返回空")
        void shouldReturnEmpty() {
            User user = sampleUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            List<AssignedRoleResponse> roles = service.getUserRoles(USER_ID);
            assertTrue(roles.isEmpty());
        }

        @Test
        @DisplayName("应映射角色")
        void shouldMapRoles() {
            User user = sampleUser();
            user.assignRoles(Set.of(1L, 2L));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            Role r1 = Role.create("Admin", "admin", null, 1);
            Role r2 = Role.create("Op", "op", null, 2);
            when(roleRepository.findAllById(any())).thenReturn(List.of(r1, r2));

            List<AssignedRoleResponse> roles = service.getUserRoles(USER_ID);

            assertEquals(2, roles.size());
            assertEquals("ENABLE", roles.get(0).status());
        }
    }
}
