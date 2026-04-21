package com.eagle.system.upms.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户已分配岗位响应 DTO
 *
 * @author sunshixiong
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedPostResponse {

    /** 岗位 ID */
    private Long id;

    /** 岗位编码 */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 岗位排序 */
    private Integer postSort;
}
