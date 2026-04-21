package com.eagle.system.system.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户所属部门响应 DTO
 *
 * @author sunshixiong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedDeptResponse {

    /** 部门 ID */
    private Long id;

    /** 父级部门 ID */
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 部门层级路径 */
    private String deptPath;

    /** 部门层级 */
    private Integer level;

    /** 部门状态：DeptStatus 枚举名称 */
    private String status;
}
