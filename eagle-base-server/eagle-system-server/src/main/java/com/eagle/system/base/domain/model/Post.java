package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.base.domain.model.enums.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 岗位实体（充血模型）
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "sys_post", comment = "岗位信息表", indexes = {
        @Index(name = "idx_post_code", columnList = "post_code", unique = true)
})
public class Post extends BaseAggregateRoot<Post> {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, comment = "岗位状态")
    private PostStatus status = PostStatus.ENABLE;

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建岗位（静态工厂方法）
     */
    public static Post create(String postCode, String postName, Integer postSort, String remark) {
        Post post = new Post();
        post.postCode = postCode;
        post.postName = postName;
        post.postSort = postSort;
        post.remark = remark;
        return post;
    }

    /**
     * 更新岗位信息
     */
    public void updateInfo(String postName, Integer postSort, String remark) {
        if (postName != null) {
            this.postName = postName;
        }
        if (postSort != null) {
            this.postSort = postSort;
        }
        if (remark != null) {
            this.remark = remark;
        }
    }

    /**
     * 更新排序
     */
    public void updateSort(Integer postSort) {
        this.postSort = postSort;
    }

    /** 启用岗位 */
    public void enable() {
        if (this.status == PostStatus.ENABLE) {
            return;  // already enabled, no-op
        }
        this.status = PostStatus.ENABLE;
    }

    /** 禁用岗位 */
    public void disable() {
        if (this.status == PostStatus.DISABLE) {
            return;  // already disabled, no-op
        }
        this.status = PostStatus.DISABLE;
    }
}
