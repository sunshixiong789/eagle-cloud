package com.eagle.auth.core.infrastructure.config;

import com.eagle.auth.core.config.AdminProperties;
import com.eagle.auth.core.domain.model.Account;
import com.eagle.auth.core.domain.model.valueobject.ProfileHints;
import com.eagle.auth.core.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员账号初始化器
 * <p>
 * 应用启动时自动创建初始管理员账号:
 * <ul>
 *   <li>检查管理员账号是否已存在,防止重复创建</li>
 *   <li>使用配置文件中的管理员信息创建 Account</li>
 *   <li>发布 AccountRegisteredEvent 事件,触发 system 域创建 User</li>
 *   <li>密码从环境变量 EAGLE_ADMIN_PASSWORD 读取,默认为 123456</li>
 * </ul>
 * <p>
 * 安全提示: 生产环境务必通过环境变量修改默认密码
 *
 * @author sunshixiong
 */
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        String username = adminProperties.getUsername();

        // 幂等性检查:防止重复创建管理员账号
        if (accountRepository.findByUsername(username).isPresent()) {
            log.debug("管理员账号已存在,跳过初始化, username: {}", username);
            return;
        }

        // 加密密码
        String encodedPassword = passwordEncoder.encode(adminProperties.getPassword());
        String phone = adminProperties.getPhone();

        // 处理可选字段(空字符串转为 null)
        String email = adminProperties.getEmail().isBlank()
                ? null : adminProperties.getEmail();
        ProfileHints hints = new ProfileHints(null, null, email);

        // 创建管理员 Account (会触发 @PostPersist 发布 AccountRegisteredEvent)
        Account account = Account.create(
                username, encodedPassword,
                phone.isBlank() ? null : phone, hints);

        Account saved = accountRepository.save(account);
        log.info("管理员账号初始化成功, username: {}, accountId: {}", username, saved.getId());
    }
}
