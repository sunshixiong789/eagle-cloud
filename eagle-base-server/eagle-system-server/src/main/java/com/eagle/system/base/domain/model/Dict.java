package com.eagle.system.base.domain.model;

import com.eagle.common.base.BaseAggregateRoot;
import com.eagle.common.base.BaseEntity;
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

import java.util.List;

/**
 * 字典类型
 *
 * @author sunshixiong
 */
@Getter
@Entity
@Table(name = "sys_dict", comment = "字典类型表", indexes = {
        @Index(name = "idx_dict_type", columnList = "dict_type", unique = true)
})
public class Dict extends BaseAggregateRoot<Dict> {

    @OneToMany(mappedBy = "dictId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DictItemEntity> dictItems;

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
}
