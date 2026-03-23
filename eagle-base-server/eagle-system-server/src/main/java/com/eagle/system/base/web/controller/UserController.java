package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.UserApplicationService;
import com.eagle.system.base.web.dto.request.ChangePasswordRequest;
import com.eagle.system.base.web.dto.request.CreateUserRequest;
import com.eagle.system.base.web.dto.request.RegisterRequest;
import com.eagle.system.base.web.dto.request.UpdateUserRequest;
import com.eagle.system.base.web.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserApplicationService userApplicationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userApplicationService.register(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return userApplicationService.createUser(request);
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userApplicationService.updateUser(id, request);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        userApplicationService.changePassword(id, request.getNewPassword());
    }

    @PutMapping("/{id}/lock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lockUser(@PathVariable Long id, @RequestParam(required = false) String reason) {
        userApplicationService.lockUser(id, reason);
    }

    @PutMapping("/{id}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlockUser(@PathVariable Long id) {
        userApplicationService.unlockUser(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userApplicationService.deleteUser(id);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        return userApplicationService.getUserById(id);
    }

    @GetMapping
    public Page<UserResponse> queryUsers(Pageable pageable) {
        return userApplicationService.queryUsers(pageable);
    }
}
