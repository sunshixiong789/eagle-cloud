package com.eagle.system.base.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 管理员用户名配置（system 侧）。
 *
 * <p>用于消费 auth-service 的 {@code AccountRegisteredMessage} 时,识别哪个 username 应被
 * 赋予 admin 角色。auth-service 的 {@code AdminInitializer} 同样依据 {@code eagle.admin.username}
 * 创建初始 Account,两侧通过相同环境变量 {@code EAGLE_ADMIN_USERNAME} 注入保持一致。
 *
 * <p>本类故意只声明 {@code username},不包含 password/email 等 auth 侧职责字段,
 * 防止 system-service 误以为自己负责 admin 账号生命周期。
 *
 * @author sunshixiong
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "eagle.admin")
public class AdminProperties {

    /** 管理员用户名,默认 {@code admin},通过 {@code EAGLE_ADMIN_USERNAME} 环境变量覆盖。 */
    private String username = "admin";
}
