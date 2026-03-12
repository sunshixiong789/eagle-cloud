package com.eleganteer.system.system.application.service;

import com.eleganteer.eleganteer.system.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 密码重置应用服务
 *
 * @author your-name
 */
@Service
@RequiredArgsConstructor
public class PasswordResetApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 通过手机号或邮箱重置密码
     *
     * @param contact     联系方式（手机号或邮箱）
     * @param newPassword 新密码
     * @return 重置结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(String contact, String newPassword) {
        return true;
    }
}