package com.eagle.system.message.interfaces.controller;

import com.eagle.resource.server.util.SecurityUtils;
import com.eagle.system.message.application.service.MessageQueryApplicationService;
import com.eagle.system.message.interfaces.dto.UserMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 站内消息用户侧接口。
 *
 * <p>仅暴露查询/已读管理——发送消息**不通过本接口**，业务方发布
 * {@code SendUserMessageIntegrationEvent} 即可。
 *
 * @author sunshixiong
 */
@Tag(name = "站内消息", description = "用户消息列表与已读管理")
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class UserMessageController {

    private final MessageQueryApplicationService messageQueryApplicationService;

    @Operation(summary = "消息列表（分页，按时间倒序）")
    @GetMapping
    public Page<UserMessageResponse> list(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return messageQueryApplicationService.listMy(userId, pageable);
    }

    @Operation(summary = "未读数")
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Map.of("count", messageQueryApplicationService.countUnread(userId));
    }

    @Operation(summary = "标记单条已读")
    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        messageQueryApplicationService.markRead(userId, id);
    }

    @Operation(summary = "全部标记已读")
    @PatchMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Map.of("updated", messageQueryApplicationService.markAllRead(userId));
    }
}
