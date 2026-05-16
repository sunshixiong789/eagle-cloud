package com.eagle.system.base.interfaces.controller;

import com.eagle.system.base.application.service.DictApplicationService;
import com.eagle.system.base.interfaces.dto.request.CreateDictItemRequest;
import com.eagle.system.base.interfaces.dto.request.CreateDictRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateDictItemRequest;
import com.eagle.system.base.interfaces.dto.request.UpdateDictRequest;
import com.eagle.system.base.interfaces.dto.response.DictItemResponse;
import com.eagle.system.base.interfaces.dto.response.DictResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 字典管理控制器
 * <p>
 * 字典项作为字典聚合内的子实体，其接口嵌套在字典资源下：
 * {@code /api/dicts/{dictId}/items}
 *
 * @author sunshixiong
 */
@Tag(name = "字典管理", description = "字典及字典项的增删改查")
@RestController
@RequestMapping("dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictApplicationService dictApplicationService;

    // ==================== 字典操作 ====================

    @Operation(summary = "创建字典", description = "创建新的字典")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public DictResponse createDict(@Valid @RequestBody CreateDictRequest request) {
        return dictApplicationService.createDict(request);
    }

    @Operation(summary = "更新字典", description = "更新指定字典信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public DictResponse updateDict(@Parameter(description = "字典ID") @PathVariable Long id, @Valid @RequestBody UpdateDictRequest request) {
        return dictApplicationService.updateDict(id, request);
    }

    @Operation(summary = "删除字典", description = "删除指定字典")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deleteDict(@Parameter(description = "字典ID") @PathVariable Long id) {
        dictApplicationService.deleteDict(id);
    }

    @Operation(summary = "查询字典详情", description = "根据 ID 获取字典详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public DictResponse getDictById(@Parameter(description = "字典ID") @PathVariable Long id) {
        return dictApplicationService.getDictById(id);
    }

    @Operation(summary = "查询字典列表", description = "分页查询所有字典")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<DictResponse> queryDict(@ParameterObject
                                         @Parameter(description = "分页参数（page=页码从0开始, size=每页条数, sort=排序字段）")
                                         @PageableDefault Pageable pageable) {
        return dictApplicationService.queryDict(pageable);
    }

    @Operation(summary = "根据类型查询字典", description = "根据字典类型获取字典信息")
    @GetMapping("/type/{dictType}")
    @PreAuthorize("isAuthenticated()")
    public DictResponse getDictByType(@Parameter(description = "字典类型") @PathVariable String dictType) {
        return dictApplicationService.getDictByType(dictType);
    }

    @Operation(summary = "批量查询字典", description = "根据多个字典类型批量查询")
    @GetMapping("/types")
    @PreAuthorize("isAuthenticated()")
    public List<DictResponse> getDictByTypes(@Parameter(description = "字典类型列表") @RequestParam List<String> dictTypes) {
        return dictApplicationService.getDictByTypes(dictTypes);
    }

    @Operation(summary = "启用字典", description = "启用指定的字典")
    @PatchMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void activateDict(@Parameter(description = "字典ID") @PathVariable Long id) {
        dictApplicationService.activateDict(id);
    }

    @Operation(summary = "禁用字典", description = "禁用指定的字典")
    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deactivateDict(@Parameter(description = "字典ID") @PathVariable Long id) {
        dictApplicationService.deactivateDict(id);
    }

    // ==================== 字典项操作（嵌套资源）====================

    @Operation(summary = "创建字典项", description = "在指定字典下创建新的字典项")
    @PostMapping("/{dictId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public DictItemResponse createDictItem(@Parameter(description = "字典ID") @PathVariable Long dictId,
                                           @Valid @RequestBody CreateDictItemRequest request) {
        return dictApplicationService.createDictItem(dictId, request);
    }

    @Operation(summary = "更新字典项", description = "更新指定字典项信息")
    @PutMapping("/{dictId}/items/{itemId}")
    @PreAuthorize("hasRole('admin')")
    public DictItemResponse updateDictItem(@Parameter(description = "字典ID") @PathVariable Long dictId,
                                           @Parameter(description = "字典项ID") @PathVariable Long itemId,
                                           @Valid @RequestBody UpdateDictItemRequest request) {
        return dictApplicationService.updateDictItem(dictId, itemId, request);
    }

    @Operation(summary = "删除字典项", description = "删除指定的字典项")
    @DeleteMapping("/{dictId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deleteDictItem(@Parameter(description = "字典ID") @PathVariable Long dictId, @Parameter(description = "字典项ID") @PathVariable Long itemId) {
        dictApplicationService.deleteDictItem(dictId, itemId);
    }

    @Operation(summary = "查询字典项详情", description = "根据 ID 获取字典项详细信息")
    @GetMapping("/{dictId}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public DictItemResponse getDictItemById(@Parameter(description = "字典ID") @PathVariable Long dictId, @Parameter(description = "字典项ID") @PathVariable Long itemId) {
        return dictApplicationService.getDictItemById(dictId, itemId);
    }

    @Operation(summary = "查询字典项列表", description = "获取指定字典下的所有字典项")
    @GetMapping("/{dictId}/items")
    @PreAuthorize("isAuthenticated()")
    public List<DictItemResponse> queryDictItems(@Parameter(description = "字典ID") @PathVariable Long dictId) {
        return dictApplicationService.queryDictItems(dictId);
    }

    @Operation(summary = "启用字典项", description = "启用指定的字典项")
    @PatchMapping("/{dictId}/items/{itemId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void activateDictItem(@Parameter(description = "字典ID") @PathVariable Long dictId, @Parameter(description = "字典项ID") @PathVariable Long itemId) {
        dictApplicationService.activateDictItem(dictId, itemId);
    }

    @Operation(summary = "禁用字典项", description = "禁用指定的字典项")
    @PatchMapping("/{dictId}/items/{itemId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deactivateDictItem(@Parameter(description = "字典ID") @PathVariable Long dictId, @Parameter(description = "字典项ID") @PathVariable Long itemId) {
        dictApplicationService.deactivateDictItem(dictId, itemId);
    }
}
