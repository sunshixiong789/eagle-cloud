package com.eagle.system.auth.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * 管理员创建账号请求
 * <p>
 * 包含 profileHints 字段，通过事件传递给 system 域，
 * 使 system 域可以在创建 User 时直接分配部门和角色。
 *
 * @author sunshixiong
 */
@Data
@Schema(description = "管理员创建账号请求")
public class CreateAccountRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 128, message = "密码长度必须在6-128个字符之间")
    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    // ==================== Profile Hints（system 域使用）====================

    /** 邮箱（可选） */
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /** 昵称（可选） */
    @Schema(description = "昵称", example = "小张")
    private String nickname;

    /** 真实姓名（可选） */
    @Schema(description = "真实姓名", example = "张三")
    private String name;

    /** 部门 ID（可选，system 域自动分配） */
    @Schema(description = "部门ID", example = "1")
    private Long deptId;

    /** 角色 ID 集合（可选，system 域自动分配） */
    @Schema(description = "角色ID集合", example = "[1, 2]")
    private Set<Long> roleIds;
}
