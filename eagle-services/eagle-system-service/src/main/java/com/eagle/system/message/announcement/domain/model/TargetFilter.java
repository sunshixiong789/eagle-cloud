package com.eagle.system.message.announcement.domain.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 公告受众过滤条件 VO（不可变）。
 *
 * <p>与 {@link TargetType} 配合：
 * <ul>
 *   <li>{@code ALL}：忽略本对象，所有用户匹配</li>
 *   <li>{@code ROLE}：用户角色与 {@link #roles} 任一交集即匹配</li>
 *   <li>{@code TAG}：用户标签与 {@link #tags} 任一交集即匹配</li>
 * </ul>
 *
 * <p>JSON 序列化后存入 {@code announcement.target_filter} 字段。
 *
 * @author sunshixiong
 */
public record TargetFilter(List<String> roles, List<String> tags) {

    public TargetFilter {
        roles = roles == null ? List.of() : List.copyOf(roles);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public static TargetFilter empty() {
        return new TargetFilter(List.of(), List.of());
    }

    public static TargetFilter ofRoles(List<String> roles) {
        return new TargetFilter(roles, List.of());
    }

    public static TargetFilter ofTags(List<String> tags) {
        return new TargetFilter(List.of(), tags);
    }

    /**
     * 当前用户的角色集合是否命中本过滤器。
     */
    @JSONField(serialize = false)
    public boolean matchesRoles(Set<String> userRoles) {
        if (roles.isEmpty() || userRoles == null || userRoles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(userRoles::contains);
    }

    /**
     * 当前用户的标签集合是否命中本过滤器。
     */
    @JSONField(serialize = false)
    public boolean matchesTags(Set<String> userTags) {
        if (tags.isEmpty() || userTags == null || userTags.isEmpty()) {
            return false;
        }
        return tags.stream().anyMatch(userTags::contains);
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public static TargetFilter fromJson(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }
        TargetFilter parsed = JSON.parseObject(json, TargetFilter.class);
        return parsed != null ? parsed : empty();
    }
}
