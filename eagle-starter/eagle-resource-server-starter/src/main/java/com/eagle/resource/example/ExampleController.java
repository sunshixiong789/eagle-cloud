package com.eagle.resource.example;

import com.eagle.common.dto.EagleUser;
import com.eagle.resource.util.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资源服务器示例控制器
 * 演示如何使用资源服务器的安全功能
 *
 * @author 孙士雄
 */
@RestController
@RequestMapping("/api/example")
public class ExampleController {

    /**
     * 公开端点（不需要认证）
     * 需要在配置中添加 /api/example/public 到 public-paths
     */
    @GetMapping("/public")
    public String publicEndpoint() {
        return "This is a public endpoint, no authentication required";
    }

    /**
     * 需要认证的端点
     */
    @GetMapping("/authenticated")
    public String authenticatedEndpoint() {
        String username = SecurityUtils.getCurrentUsername();
        return "Hello, " + username + "! You are authenticated.";
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public EagleUser getCurrentUser() {
        return SecurityUtils.getCurrentUser();
    }

    /**
     * 需要 ADMIN 角色
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminEndpoint() {
        return "This endpoint requires ADMIN role";
    }

    /**
     * 需要 ADMIN 或 MANAGER 角色
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/management")
    public String managementEndpoint() {
        return "This endpoint requires ADMIN or MANAGER role";
    }

    /**
     * 编程式权限检查
     */
    @GetMapping("/check-role")
    public String checkRole() {
        if (SecurityUtils.hasRole("ADMIN")) {
            return "You have ADMIN role";
        } else if (SecurityUtils.hasRole("USER")) {
            return "You have USER role";
        } else {
            return "You don't have ADMIN or USER role";
        }
    }
}
