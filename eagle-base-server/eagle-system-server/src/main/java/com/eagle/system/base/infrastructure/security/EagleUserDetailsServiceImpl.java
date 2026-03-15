package com.eagle.system.base.infrastructure.security;

import com.eagle.common.dto.EagleUser;
import com.eagle.system.domain.model.Dept;
import com.eagle.system.domain.model.Role;
import com.eagle.system.domain.model.User;
import com.eagle.system.domain.repository.DeptRepository;
import com.eagle.system.domain.repository.RoleRepository;
import com.eagle.system.domain.repository.UserRepository;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Security UserDetailsService 实现
 * <p>
 * 改进：适配聚合根边界，通过 ID 查询关联对象
 *
 * @author 孙士雄
 */
@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class EagleUserDetailsServiceImpl implements UserDetailsService {

    public static String ROLE = "ROLE_";
    private final UserRepository userRepository;
    private final DeptRepository deptRepository;
    private final RoleRepository roleRepository;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        // 测试用
        if ("admin".equals(username)) {
            return new EagleUser(2121L, "admin", new BCryptPasswordEncoder().encode("123456"),
                    "孙士雄", 23231L, "技术部", "17708080863",
                    AuthorityUtils.createAuthorityList("USER", "ADMIN"));
        }
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            Set<String> dbAuthsSet = Sets.newHashSet();
            User user = userOptional.get();

            // 通过 deptId 查询部门（聚合根之间通过 ID 引用）
            Dept dept = null;
            String deptName = "";
            Long deptId = null;
            if (user.getDeptId() != null) {
                dept = deptRepository.findById(user.getDeptId()).orElse(null);
                if (dept != null) {
                    deptId = dept.getId();
                    deptName = dept.getName();
                }
            }

            // 通过 roleIds 查询角色列表（聚合根之间通过 ID 引用）
            if (user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
                List<Role> roles = roleRepository.findAllById(user.getRoleIds());
                List<String> roleCodes = roles.stream()
                        .map(Role::getRoleCode)
                        .toList();
                roleCodes.forEach(roleCode -> dbAuthsSet.add(ROLE + roleCode));
            }

            return new EagleUser(
                    user.getId(),
                    user.getUsername(),
                    user.getPassword(),
                    user.getUsername(),
                    deptId,
                    deptName,
                    user.getPhone(),
                    AuthorityUtils.createAuthorityList(dbAuthsSet.toArray(new String[0]))
            );
        } else {
            throw new UsernameNotFoundException("用户不存在");
        }
    }
}
