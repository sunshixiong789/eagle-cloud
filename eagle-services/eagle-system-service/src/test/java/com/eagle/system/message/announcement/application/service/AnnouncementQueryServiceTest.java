package com.eagle.system.message.announcement.application.service;

import com.eagle.system.message.announcement.domain.model.AnnouncementCategory;
import com.eagle.system.message.announcement.domain.model.TargetFilter;
import com.eagle.system.message.announcement.domain.model.TargetType;
import com.eagle.system.message.announcement.domain.model.UserAnnouncementCursor;
import com.eagle.system.message.announcement.domain.repository.UserAnnouncementCursorRepository;
import com.eagle.system.message.announcement.infrastructure.cache.AnnouncementCache;
import com.eagle.system.message.announcement.infrastructure.cache.AnnouncementSnapshot;
import com.eagle.system.message.announcement.interfaces.dto.AnnouncementView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementQueryService")
class AnnouncementQueryServiceTest {

    private static final Long USER_ID = 100L;
    private static final LocalDateTime T1 = LocalDateTime.of(2026, 5, 1, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2026, 5, 10, 10, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2026, 5, 20, 10, 0);

    @Mock
    private AnnouncementCache announcementCache;

    @Mock
    private UserAnnouncementCursorRepository cursorRepository;

    @InjectMocks
    private AnnouncementQueryService service;

    @Nested
    @DisplayName("listForUser")
    class ListForUser {

        @BeforeEach
        void setupActiveAnnouncements() {
            // 三条公告：ALL/ROLE(admin)/TAG(vip)，时间各异
            AnnouncementSnapshot all = new AnnouncementSnapshot(1L, AnnouncementCategory.SYSTEM,
                    "全员", "x", TargetType.ALL, null, T1, null);
            AnnouncementSnapshot adminOnly = new AnnouncementSnapshot(2L, AnnouncementCategory.MAINTENANCE,
                    "管理员", "x", TargetType.ROLE,
                    TargetFilter.ofRoles(List.of("admin")).toJson(), T2, null);
            AnnouncementSnapshot vipOnly = new AnnouncementSnapshot(3L, AnnouncementCategory.ACTIVITY,
                    "VIP", "x", TargetType.TAG,
                    TargetFilter.ofTags(List.of("vip")).toJson(), T3, null);
            when(announcementCache.loadActive()).thenReturn(List.of(all, adminOnly, vipOnly));
        }

        @Test
        @DisplayName("普通用户：只见 ALL")
        void plainUserSeesAllOnly() {
            when(announcementCache.getCursor(USER_ID)).thenReturn(LocalDateTime.MIN);

            List<AnnouncementView> result = service.listForUser(USER_ID, Set.of("user"), Set.of());

            assertThat(result).extracting(AnnouncementView::id).containsExactly(1L);
        }

        @Test
        @DisplayName("admin 角色：见 ALL + admin 公告，按 publish_time 降序")
        void adminSeesAllAndAdminOnly() {
            when(announcementCache.getCursor(USER_ID)).thenReturn(LocalDateTime.MIN);

            List<AnnouncementView> result = service.listForUser(USER_ID, Set.of("admin"), Set.of());

            assertThat(result).extracting(AnnouncementView::id).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("vip 标签：见 ALL + vip 公告")
        void vipSeesAllAndVipOnly() {
            when(announcementCache.getCursor(USER_ID)).thenReturn(LocalDateTime.MIN);

            List<AnnouncementView> result = service.listForUser(USER_ID, Set.of(), Set.of("vip"));

            assertThat(result).extracting(AnnouncementView::id).containsExactly(3L, 1L);
        }

        @Test
        @DisplayName("已读标记：游标 ≥ publish_time 的视为已读")
        void readMarksByCursor() {
            when(announcementCache.getCursor(USER_ID)).thenReturn(T2);

            List<AnnouncementView> result = service.listForUser(USER_ID, Set.of("admin"), Set.of());

            // T2 公告 isRead=true，T1 公告 isRead=true（都不晚于 cursor），T3 不可见
            assertThat(result).extracting(AnnouncementView::id, AnnouncementView::isRead)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(2L, true),
                            org.assertj.core.groups.Tuple.tuple(1L, true)
                    );
        }
    }

    @Nested
    @DisplayName("countUnread")
    class CountUnread {

        @Test
        @DisplayName("仅可见且未读的计入")
        void onlyVisibleAndUnread() {
            AnnouncementSnapshot all = new AnnouncementSnapshot(1L, AnnouncementCategory.SYSTEM,
                    "x", "x", TargetType.ALL, null, T1, null);
            AnnouncementSnapshot adminOnly = new AnnouncementSnapshot(2L, AnnouncementCategory.SYSTEM,
                    "x", "x", TargetType.ROLE,
                    TargetFilter.ofRoles(List.of("admin")).toJson(), T2, null);
            when(announcementCache.loadActive()).thenReturn(List.of(all, adminOnly));
            when(announcementCache.getCursor(USER_ID)).thenReturn(T1); // 已读 T1，未读晚于 T1 的

            long unread = service.countUnread(USER_ID, Set.of("admin"), Set.of());

            assertThat(unread).isEqualTo(1L); // 只有 T2 admin 公告未读
        }
    }

    @Nested
    @DisplayName("markAllRead")
    class MarkAllRead {

        @Test
        @DisplayName("游标推进到最大可见 publish_time，新建 cursor")
        void createsCursorIfMissing() {
            AnnouncementSnapshot all = new AnnouncementSnapshot(1L, AnnouncementCategory.SYSTEM,
                    "x", "x", TargetType.ALL, null, T2, null);
            when(announcementCache.loadActive()).thenReturn(List.of(all));
            when(cursorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(cursorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.markAllRead(USER_ID, Set.of(), Set.of());

            verify(cursorRepository).save(any(UserAnnouncementCursor.class));
            verify(announcementCache).setCursor(USER_ID, T2);
        }

        @Test
        @DisplayName("无可见公告：仍持久化 now 游标避免重复处理")
        void emptyActiveStillSaves() {
            when(announcementCache.loadActive()).thenReturn(List.of());
            when(cursorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(cursorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.markAllRead(USER_ID, Set.of(), Set.of());

            verify(cursorRepository).save(any(UserAnnouncementCursor.class));
        }
    }
}
