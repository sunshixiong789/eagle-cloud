package com.eagle.common.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
     * 部门 ID（序列化为 String 防止前端精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private final Long deptId;

    /**
     * 部门名称
     */
    private final String deptName;

    /**
     * 手机号
     */
    private final String phone;

    public EagleUser(Long id, String username, String password, String name, Long deptId, String deptName, String phone, boolean enabled,
                     boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.name = name;
        this.deptId = deptId;
        this.deptName = deptName;
        this.phone = phone;
    }

    public EagleUser(Long id, String username, String password, String name, Long deptId, String deptName, String phone, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, true, true, true, true, authorities);
        this.id = id;
        this.name = name;
        this.deptId = deptId;
        this.deptName = deptName;
        this.phone = phone;
    }
}
