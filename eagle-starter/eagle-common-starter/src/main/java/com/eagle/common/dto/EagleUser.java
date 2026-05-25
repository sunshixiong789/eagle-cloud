package com.eagle.common.dto;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;


/**
 * Eagle 系统用户认证信息
 * <p>
 * 扩展 Spring Security 的 User 类，添加业务所需的用户信息。
 * Long 类型 ID 使用 ToStringSerializer 防止前端 JavaScript 精度丢失。
 *
 * @author 孙士雄
 */
@Getter
public class EagleUser extends User {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 扩展属性，方便存放 OAuth2 上下文相关信息
     */
    private final HashMap<String, Serializable> attributes = new HashMap<>();

    /**
     * 用户 ID（序列化为 String 防止前端精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private final Long id;

    /**
     * 用户姓名
     */
    private final String name;

    /**
     * 手机号
     */
    private final String phone;

    /**
     * 头像 URL
     */
    private final String avatar;

    public EagleUser(Long id, String username, String password, String name, String phone, String avatar, boolean enabled,
                     boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.avatar = avatar;
    }

    public EagleUser(Long id, String username, String password, String name, String phone, String avatar,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, true, true, true, true, authorities);
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.avatar = avatar;
    }
}
