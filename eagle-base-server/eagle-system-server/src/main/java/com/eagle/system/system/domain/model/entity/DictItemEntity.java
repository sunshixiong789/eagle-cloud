package com.eagle.system.system.domain.model.entity;

import com.eagle.common.base.BaseEntity;
import com.eagle.system.domain.model.enums.DictStatus;
import com.eagle.system.domain.model.enums.DictType;
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
import lombok.Setter;

/**
 * 字典项
 *
 * @author sunshixiong
 */

@Getter
@Setter
@Entity
@Table(name = "sys_dict_item", comment = "字典项表", indexes = {
        @Index(name = "idx_dict_id", columnList = "dict_id"),
        @Index(name = "idx_dict_type", columnList = "dict_type"),
        @Index(name = "idx_item_value", columnList = "item_value"),
        @Index(name = "idx_parent_id", columnList = "parent_id")
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
    @Enumerated(EnumType.STRING)
    private DictType dictType;

    @Column(name = "parent_id", nullable = false, comment = "父级字典项ID，0表示顶级")
    private Long parentId = 0L;

    @Column(length = 500, comment = "描述")
    private String description;

    @Column(comment = "排序值")
    private Integer sortOrder;

    @NotNull(message = "字典项状态不能为空")
    @Column(nullable = false, length = 20, comment = "字典项状态")
    @Enumerated(EnumType.STRING)
    private DictStatus status = DictStatus.ACTIVE;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Column(length = 500, comment = "备注")
    private String remarks;

    public static DictItemEntity create(Long dictId, String itemValue, String name,
                                        DictType dictType, Long parentId,
                                        String description, Integer sortOrder, String remarks) {
        DictItemEntity item = new DictItemEntity();
        item.dictId = dictId;
        item.itemValue = itemValue;
        item.name = name;
        item.dictType = dictType;
        item.parentId = parentId != null ? parentId : 0L;
        item.description = description;
        item.sortOrder = sortOrder;
        item.remarks = remarks;
        item.status = DictStatus.ACTIVE;
        return item;
    }

    public void updateInfo(String itemValue, String name, String description,
                           Integer sortOrder, String remarks) {
        if (itemValue != null) this.itemValue = itemValue;
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (remarks != null) this.remarks = remarks;
    }

    public void activate() {
        this.status = DictStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = DictStatus.INACTIVE;
    }
}
