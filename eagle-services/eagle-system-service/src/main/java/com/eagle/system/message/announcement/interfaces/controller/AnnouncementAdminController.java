package com.eagle.system.message.announcement.interfaces.controller;

import com.eagle.system.message.announcement.application.service.AnnouncementAdminApplicationService;
import com.eagle.system.message.announcement.interfaces.dto.AnnouncementAdminView;
import com.eagle.system.message.announcement.interfaces.dto.PublishAnnouncementRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公告后台管理接口（仅 admin 角色可访问）。
 *
 * @author sunshixiong
 */
@Tag(name = "公告管理（后台）", description = "运营发布/撤回全员公告")
@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('admin')")
public class AnnouncementAdminController {

    private final AnnouncementAdminApplicationService adminService;

    @Operation(summary = "发布公告（持久化 + 失效缓存 + 触发跨实例 WebSocket 广播）")
    @PostMapping
    public Map<String, Long> publish(@Valid @RequestBody PublishAnnouncementRequest request) {
        return Map.of("id", adminService.publish(request).getId());
    }

    @Operation(summary = "撤回公告（逻辑删除 + 失效缓存）")
    @DeleteMapping("/{id}")
    public void revoke(@PathVariable Long id) {
        adminService.revoke(id);
    }

    @Operation(summary = "公告分页列表（含已撤回/已过期，后台审计用）")
    @GetMapping
    public Page<AnnouncementAdminView> list(Pageable pageable) {
        return adminService.listAll(pageable);
    }
}
