package com.eagle.system.message.announcement.interfaces.controller;

import com.eagle.resource.server.util.SecurityUtils;
import com.eagle.system.message.announcement.application.service.AnnouncementQueryApplicationService;
import com.eagle.system.message.announcement.interfaces.dto.AnnouncementView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公告用户侧接口。
 *
 * <p>读取性能：单请求 ≤ 2 次 Redis 读 + 内存过滤，零 SQL，
 * 适合高 QPS 公告中心场景。
 *
 * @author sunshixiong
 */
@Tag(name = "公告（用户）", description = "用户公告中心：列表、未读数、全部已读")
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserAnnouncementController {

    private final AnnouncementQueryApplicationService queryService;

    @Operation(summary = "公告列表（按 publish_time 降序，含已读标记）")
    @GetMapping
    public List<AnnouncementView> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<String> roles = CurrentUserContext.currentRoles();
        Set<String> tags = CurrentUserContext.currentTags();
        return queryService.listForUser(userId, roles, tags);
    }

    @Operation(summary = "未读公告数")
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<String> roles = CurrentUserContext.currentRoles();
        Set<String> tags = CurrentUserContext.currentTags();
        return Map.of("count", queryService.countUnread(userId, roles, tags));
    }

    @Operation(summary = "标记当前用户应见公告全部已读（推进游标到最新一条）")
    @PostMapping("/read-all")
    public void markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        Set<String> roles = CurrentUserContext.currentRoles();
        Set<String> tags = CurrentUserContext.currentTags();
        queryService.markAllRead(userId, roles, tags);
    }
}
