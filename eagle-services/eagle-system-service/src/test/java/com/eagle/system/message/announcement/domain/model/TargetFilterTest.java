package com.eagle.system.message.announcement.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TargetFilter 受众过滤")
class TargetFilterTest {

    @Test
    @DisplayName("空过滤器：matches 始终 false")
    void emptyFilterMatchesNothing() {
        TargetFilter f = TargetFilter.empty();
        assertThat(f.matchesRoles(Set.of("admin"))).isFalse();
        assertThat(f.matchesTags(Set.of("vip"))).isFalse();
    }

    @Test
    @DisplayName("matchesRoles：交集非空即命中")
    void matchesRolesByIntersection() {
        TargetFilter f = TargetFilter.ofRoles(List.of("admin", "vip"));
        assertThat(f.matchesRoles(Set.of("user", "admin"))).isTrue();
        assertThat(f.matchesRoles(Set.of("user"))).isFalse();
        assertThat(f.matchesRoles(Set.of())).isFalse();
        assertThat(f.matchesRoles(null)).isFalse();
    }

    @Test
    @DisplayName("matchesTags：交集非空即命中")
    void matchesTagsByIntersection() {
        TargetFilter f = TargetFilter.ofTags(List.of("new_user", "premium"));
        assertThat(f.matchesTags(Set.of("premium"))).isTrue();
        assertThat(f.matchesTags(Set.of("legacy"))).isFalse();
    }

    @Test
    @DisplayName("JSON 往返：序列化后反序列化等价")
    void jsonRoundTrip() {
        TargetFilter f = new TargetFilter(List.of("admin"), List.of("vip"));
        String json = f.toJson();
        TargetFilter back = TargetFilter.fromJson(json);
        assertThat(back.roles()).containsExactly("admin");
        assertThat(back.tags()).containsExactly("vip");
    }

    @Test
    @DisplayName("fromJson 处理 null/blank：返回 empty")
    void fromJsonHandlesNullBlank() {
        assertThat(TargetFilter.fromJson(null).roles()).isEmpty();
        assertThat(TargetFilter.fromJson("").roles()).isEmpty();
        assertThat(TargetFilter.fromJson("  ").tags()).isEmpty();
    }
}
