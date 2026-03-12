package com.eagle.system.common.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * @author 孙士雄 15:03
 */
@Getter
@Setter
public class EagleUser extends User {

    /**
     * 扩展属性，方便存放oauth 上下文相关信息
     */
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 用户ID
     */
    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Long id;
    /**
     * 用户名
     */
    @Getter
    private final String name;

    /**
     * 部门ID
     */
    @Getter
    @JsonSerialize(using = ToStringSerializer.class)
    private final Long deptId;

    /**
     * 部门ID
     */
    @Getter
    private final String deptName;

    /**
     * 手机号
     */
    @Getter
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
