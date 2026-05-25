package com.eagle.auth.infrastructure.security;

import com.eagle.auth.domain.service.PasswordEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt 密码加密器实现
 * <p>
 * 基础设施层实现领域层接口
 *
 * @author eagle
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class BCryptPasswordEncryptor implements PasswordEncryptor {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encrypt(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    @Override
    public boolean matches(String plainPassword, String encryptedPassword) {
        return passwordEncoder.matches(plainPassword, encryptedPassword);
    }
}
