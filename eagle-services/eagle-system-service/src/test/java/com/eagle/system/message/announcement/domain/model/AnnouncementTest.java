package com.eagle.system.message.announcement.domain.model;

import com.eagle.common.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Announcement 聚合根")
class AnnouncementTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 21, 12, 0);

    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("ALL 类型：targetFilterJson 置 null（不需要受众数据）")
        void allTypeClearsFilter() {
            Announcement a = Announcement.publish(
                    AnnouncementCategory.SYSTEM, "t", "c",
                    TargetType.ALL, TargetFilter.ofRoles(List.of("admin")),
                    NOW, null);
            assertThat(a.getTargetFilterJson()).isNull();
        }

        @Test
        @DisplayName("ROLE 类型：保留 targetFilterJson")
        void roleTypeKeepsFilter() {
            Announcement a = Announcement.publish(
                    AnnouncementCategory.MAINTENANCE, "t", "c",
                    TargetType.ROLE, TargetFilter.ofRoles(List.of("admin")),
                    NOW, null);
            assertThat(a.getTargetFilterJson()).contains("admin");
        }

        @Test
        @DisplayName("expireTime 早于 publishTime → DomainException")
        void rejectsInvalidExpire() {
            assertThrows(DomainException.class, () -> Announcement.publish(
                    AnnouncementCategory.SYSTEM, "t", "c",
                    TargetType.ALL, null,
                    NOW, NOW.minusHours(1)));
        }
    }

    @Nested
    @DisplayName("isActiveAt")
    class IsActiveAt {

        @Test
        @DisplayName("已发布、未过期、未撤回 → true")
        void positive() {
            Announcement a = publishAll(NOW.minusHours(1), NOW.plusHours(1));
            assertThat(a.isActiveAt(NOW)).isTrue();
        }

        @Test
        @DisplayName("publishTime 在未来 → false")
        void notYetPublished() {
            Announcement a = publishAll(NOW.plusHours(1), null);
            assertThat(a.isActiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("过期 → false")
        void expired() {
            Announcement a = publishAll(NOW.minusHours(2), NOW.minusHours(1));
            assertThat(a.isActiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("撤回 → false")
        void revoked() {
            Announcement a = publishAll(NOW.minusHours(1), null);
            a.revoke();
            assertThat(a.isActiveAt(NOW)).isFalse();
        }

        @Test
        @DisplayName("expireTime 为 null → 永久有效")
        void neverExpires() {
            Announcement a = publishAll(NOW.minusYears(5), null);
            assertThat(a.isActiveAt(NOW)).isTrue();
        }
    }

    @Nested
    @DisplayName("isVisibleTo")
    class IsVisibleTo {

        @Test
        @DisplayName("ALL：所有用户可见")
        void allVisible() {
            Announcement a = publishAll(NOW, null);
            assertThat(a.isVisibleTo(Set.of(), Set.of())).isTrue();
            assertThat(a.isVisibleTo(Set.of("user"), Set.of("any"))).isTrue();
        }

        @Test
        @DisplayName("ROLE：用户角色命中即可见")
        void roleVisibleByMatch() {
            Announcement a = Announcement.publish(
                    AnnouncementCategory.SYSTEM, "t", "c",
                    TargetType.ROLE, TargetFilter.ofRoles(List.of("admin")),
                    NOW, null);
            assertThat(a.isVisibleTo(Set.of("admin"), Set.of())).isTrue();
            assertThat(a.isVisibleTo(Set.of("user"), Set.of())).isFalse();
        }

        @Test
        @DisplayName("TAG：用户标签命中即可见")
        void tagVisibleByMatch() {
            Announcement a = Announcement.publish(
                    AnnouncementCategory.ACTIVITY, "t", "c",
                    TargetType.TAG, TargetFilter.ofTags(List.of("vip")),
                    NOW, null);
            assertThat(a.isVisibleTo(Set.of(), Set.of("vip"))).isTrue();
            assertThat(a.isVisibleTo(Set.of("vip"), Set.of())).isFalse();
        }
    }

    @Test
    @DisplayName("revoke 幂等：重复调用 revoked 仍为 true")
    void revokeIsIdempotent() {
        Announcement a = publishAll(NOW, null);
        a.revoke();
        a.revoke();
        assertThat(a.isRevoked()).isTrue();
    }

    private Announcement publishAll(LocalDateTime publishTime, LocalDateTime expireTime) {
        return Announcement.publish(AnnouncementCategory.SYSTEM, "t", "c",
                TargetType.ALL, null, publishTime, expireTime);
    }
}
