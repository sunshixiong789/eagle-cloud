package com.eagle.auth.core.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * 管理员初始化配置属性
 *
 * <p>对应 application.yml 中的 {@code eagle.admin} 前缀配置。
 * 用于应用启动时预置管理员账号。</p>
 *
 * <p>密码无默认值，必须通过环境变量 {@code EAGLE_ADMIN_PASSWORD} 或配置文件显式设置，
 * 否则应用启动时校验失败。</p>
 *
 * @author sunshixiong
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "eagle.admin")
public class AdminProperties {

    /**
     * 管理员用户名
     */
    private String username = "admin";

    /**
     * 管理员密码（无默认值，必须通过环境变量或配置文件显式设置）
     */
    @NotBlank(message = "管理员密码不能为空，请设置环境变量 EAGLE_ADMIN_PASSWORD")
    @Size(min = 8, message = "管理员密码长度不能少于 8 位")
    private String password;

    /**
     * 管理员姓名
     */
    private String name = "系统管理员";

    /**
     * 管理员手机号
     */
    private String phone = "";

    /**
     * 管理员邮箱
     */
    private String email = "admin@eagle.com";
}
