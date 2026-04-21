package com.eagle.system.auth.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理员初始化配置属性
 *
 * <p>对应 application.yml 中的 {@code eagle.admin} 前缀配置。
 * 用于应用启动时预置管理员账号。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "eagle.admin")
public class AdminProperties {

    /** 管理员用户名 */
    private String username = "admin";

    /** 管理员默认密码（仅首次初始化时使用） */
    private String password = "123456";

    /** 管理员姓名 */
    private String name = "系统管理员";

    /** 管理员手机号 */
    private String phone = "";

    /** 管理员邮箱 */
    private String email = "admin@eagle.com";
}
