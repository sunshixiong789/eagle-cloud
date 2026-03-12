package com.eleganteer.system.system.domain.model;


import com.eleganteer.eleganteer.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 岗位信息表
 *
 * @author sunshixiong
 */
@Getter
@Entity
@Table(name = "sys_post", comment = "岗位信息表", indexes = {
        @Index(name = "idx_post_code", columnList = "post_code", unique = true)
})
public class Post extends BaseEntity {

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码长度不能超过64个字符")
    @Column(nullable = false, unique = true, length = 64, comment = "岗位编码")
    private String postCode;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 64, message = "岗位名称长度不能超过64个字符")
    @Column(nullable = false, length = 64, comment = "岗位名称")
    private String postName;

    @NotNull(message = "岗位排序不能为空")
    @Column(nullable = false, comment = "岗位排序")
    private Integer postSort;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Column(length = 500, comment = "备注")
    private String remark;
}
