package com.eagle.system.base.application.service;


import com.eagle.system.base.domain.model.User;
import com.eagle.system.base.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户注册应用服务
 *
 * @author your-name
 */
@Service
@RequiredArgsConstructor
public class RegisterApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 通过手机号或邮箱注册用户
     *
     * @param phone    手机
     * @param email    邮箱
     * @param password 密码
     * @return 注册结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean registerUser(String name, String phone, String email, String password) {

        User user = new User();
       /* user.setUsername(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setEmail(email);
*/
        userRepository.save(user);
        return true;
    }
}