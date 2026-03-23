package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.system.base.domain.model.entity.DictItemEntity;
import com.eagle.system.base.domain.model.enums.DictStatus;
import com.eagle.system.base.domain.model.enums.DictType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 字典聚合根（充血模型）
 *
 * @author sunshixiong
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "sys_dict", comment = "字典类型表", indexes = {
        @Index(name = "idx_dict_type", columnList = "dict_type", unique = true)
})
public class Dict extends BaseAggregateRoot<Dict> {

    @OneToMany(mappedBy = "dictId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DictItemEntity> dictItems = new ArrayList<>();

    @NotNull(message = "字典类型不能为空")
    @Column(nullable = false, unique = true, length = 50, comment = "字典类型")
    @Enumerated
    private DictType dictType;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过100个字符")
    @Column(nullable = false, length = 100, comment = "字典名称")
    private String dictName;

    @Size(max = 255, message = "字典描述长度不能超过255个字符")
    @Column(length = 500, comment = "字典描述")
    private String description;

    @Column(nullable = false, comment = "是否系统内置")
    private Boolean systemFlag = false;

    @NotNull(message = "字典状态不能为空")
    @Column(nullable = false, length = 20, comment = "字典状态")
    @Enumerated
    private DictStatus status = DictStatus.ACTIVE;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Column(length = 500, comment = "备注")
    private String remarks;

    // ==================== 业务方法（充血模型）====================

    /**
     * 创建字典（静态工厂方法）
     */
    public static Dict create(DictType dictType, String dictName, String description, String remarks) {
        Dict dict = new Dict();
        dict.dictType = dictType;
        dict.dictName = dictName;
        dict.description = description;
        dict.remarks = remarks;
        dict.systemFlag = false;
        dict.status = DictStatus.ACTIVE;
        return dict;
    }

    /**
     * 更新字典信息
     */
    public void updateInfo(String dictName, String description, String remarks) {
        if (dictName != null) {
            this.dictName = dictName;
        }
        if (description != null) {
            this.description = description;
        }
        if (remarks != null) {
            this.remarks = remarks;
        }
    }

    /**
     * 添加字典项
     */
    public void addItem(DictItemEntity item) {
        this.dictItems.add(item);
    }

    /**
     * 移除字典项
     */
    public void removeItem(DictItemEntity item) {
        this.dictItems.remove(item);
    }

    /**
     * 激活字典
     */
    public void activate() {
        this.status = DictStatus.ACTIVE;
    }

    /**
     * 停用字典
     */
    public void deactivate() {
        this.status = DictStatus.INACTIVE;
    }

    /**
     * 判断是否为系统内置字典
     */
    public boolean isSystem() {
        return Boolean.TRUE.equals(this.systemFlag);
    }
}
