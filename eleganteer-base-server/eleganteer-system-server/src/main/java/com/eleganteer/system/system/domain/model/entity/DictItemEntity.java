package com.eleganteer.system.system.domain.model.entity;

import com.eleganteer.eleganteer.common.base.BaseEntity;
import com.eleganteer.eleganteer.system.domain.model.enums.DictStatus;
import com.eleganteer.eleganteer.system.domain.model.enums.DictType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 字典项
 *
 * @author sunshixiong
 */

@Getter
@Entity
@Table(name = "sys_dict_item", comment = "字典项表", indexes = {
        @Index(name = "idx_dict_id", columnList = "dict_id"),
        @Index(name = "idx_dict_type", columnList = "dict_type"),
        @Index(name = "idx_item_value", columnList = "item_value")
})
public class DictItemEntity extends BaseEntity {

    @NotNull
    @Column(name = "dict_id", nullable = false)
    private Long dictId;

    @NotBlank(message = "字典项值不能为空")
    @Size(max = 100, message = "字典项值长度不能超过100个字符")
    @Column(nullable = false, length = 100, comment = "字典项值")
    private String itemValue;

    @NotBlank(message = "字典项标签不能为空")
    @Size(max = 100, message = "字典项标签长度不能超过100个字符")
    @Column(nullable = false, length = 100, comment = "字典项标签")
    private String name;

    @NotNull(message = "字典类型不能为空")
    @Column(nullable = false, length = 50, comment = "字典类型")
    @Enumerated
    private DictType dictType;

    @Column(length = 500, comment = "描述")
    private String description;

    @Column(comment = "排序值")
    private Integer sortOrder;

    @NotNull(message = "字典项状态不能为空")
    @Column(nullable = false, length = 20, comment = "字典项状态")
    @Enumerated
    private DictStatus status = DictStatus.ACTIVE;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Column(length = 500, comment = "备注")
    private String remarks;
}
