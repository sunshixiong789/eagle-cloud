package com.eagle.system.upms.web.controller;

import com.eagle.system.upms.application.service.PostApplicationService;
import com.eagle.system.upms.web.dto.request.CreatePostRequest;
import com.eagle.system.upms.web.dto.request.UpdatePostRequest;
import com.eagle.system.upms.web.dto.response.PostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 岗位管理控制器
 *
 * @author sunshixiong
 */
@Tag(name = "岗位管理", description = "岗位的增删改查")
@RestController
@RequestMapping("posts")
@RequiredArgsConstructor
public class PostController {

    private final PostApplicationService postApplicationService;

    @Operation(summary = "创建岗位", description = "创建新的岗位")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest request) {
        return postApplicationService.createPost(request);
    }

    @Operation(summary = "更新岗位", description = "更新指定岗位信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public PostResponse updatePost(@Parameter(description = "岗位ID") @PathVariable Long id, @Valid @RequestBody UpdatePostRequest request) {
        return postApplicationService.updatePost(id, request);
    }

    @Operation(summary = "删除岗位", description = "删除指定岗位")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void deletePost(@Parameter(description = "岗位ID") @PathVariable Long id) {
        postApplicationService.deletePost(id);
    }

    @Operation(summary = "查询岗位详情", description = "根据 ID 获取岗位详细信息")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PostResponse getPostById(@Parameter(description = "岗位ID") @PathVariable Long id) {
        return postApplicationService.getPostById(id);
    }

    @Operation(summary = "查询岗位列表", description = "分页查询所有岗位")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<PostResponse> queryPosts(Pageable pageable) {
        return postApplicationService.queryPosts(pageable);
    }

    @Operation(summary = "启用岗位")
    @PatchMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void enablePost(@Parameter(description = "岗位ID") @PathVariable Long id) {
        postApplicationService.enablePost(id);
    }

    @Operation(summary = "禁用岗位")
    @PatchMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('admin')")
    public void disablePost(@Parameter(description = "岗位ID") @PathVariable Long id) {
        postApplicationService.disablePost(id);
    }
}
