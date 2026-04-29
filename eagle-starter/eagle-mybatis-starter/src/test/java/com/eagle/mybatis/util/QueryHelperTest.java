package com.eagle.mybatis.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link QueryHelper} 单元测试。
 */
@DisplayName("QueryHelper")
class QueryHelperTest {

    // ==================== Stub entity ====================

    /** 测试用存根实体，仅用于构建 LambdaQueryWrapper 的类型参数。 */
    @Data
    static class StubEntity {
        private Long id;
        private String name;
        private String phone;
        private Integer status;
        private LocalDateTime createTime;
    }

    // ==================== likeAny ====================

    @Nested
    @DisplayName("likeAny")
    class LikeAny {

        @Test
        @DisplayName("should append or-like conditions when keyword has text")
        void shouldAppendConditionsWhenKeywordHasText() {
            LambdaQueryWrapper<StubEntity> wrapper = QueryHelper.likeAny(
                    "张三",
                    StubEntity::getName,
                    StubEntity::getPhone
            );

            assertNotNull(wrapper);
            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have SQL segment when keyword is present");
        }

        @Test
        @DisplayName("should return wrapper with no conditions when keyword is null")
        void shouldReturnEmptyWrapperWhenKeywordIsNull() {
            LambdaQueryWrapper<StubEntity> wrapper = QueryHelper.likeAny(
                    null,
                    StubEntity::getName
            );

            assertNotNull(wrapper);
            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when keyword is null");
        }

        @Test
        @DisplayName("should return wrapper with no conditions when keyword is blank")
        void shouldReturnEmptyWrapperWhenKeywordIsBlank() {
            LambdaQueryWrapper<StubEntity> wrapper = QueryHelper.likeAny(
                    "   ",
                    StubEntity::getName
            );

            assertNotNull(wrapper);
            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when keyword is blank");
        }

        @Test
        @DisplayName("should return empty wrapper when keyword is empty string")
        void shouldReturnEmptyWrapperWhenKeywordIsEmpty() {
            LambdaQueryWrapper<StubEntity> wrapper = QueryHelper.likeAny(
                    "",
                    StubEntity::getName
            );

            assertNotNull(wrapper);
            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when keyword is empty string");
        }

        @Test
        @DisplayName("should return empty wrapper when no columns are provided")
        void shouldReturnEmptyWrapperWhenNoColumns() {
            LambdaQueryWrapper<StubEntity> wrapper = QueryHelper.likeAny("keyword");

            assertNotNull(wrapper);
            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when no columns are provided");
        }
    }

    // ==================== likeIfPresent ====================

    @Nested
    @DisplayName("likeIfPresent")
    class LikeIfPresent {

        @Test
        @DisplayName("should append like condition when value has text")
        void shouldAppendConditionWhenValueHasText() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.likeIfPresent(wrapper, StubEntity::getName, "张三");

            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have SQL segment when value is present");
        }

        @Test
        @DisplayName("should not append like condition when value is null")
        void shouldNotAppendConditionWhenValueIsNull() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.likeIfPresent(wrapper, StubEntity::getName, null);

            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when value is null");
        }

        @Test
        @DisplayName("should not append like condition when value is blank")
        void shouldNotAppendConditionWhenValueIsBlank() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.likeIfPresent(wrapper, StubEntity::getName, "  ");

            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when value is blank");
        }
    }

    // ==================== dateBetween ====================

    @Nested
    @DisplayName("dateBetween")
    class DateBetween {

        private static final LocalDateTime START = LocalDateTime.of(2024, 1, 1, 0, 0);
        private static final LocalDateTime END = LocalDateTime.of(2024, 12, 31, 23, 59);

        @Test
        @DisplayName("should append both ge and le conditions when start and end are both present")
        void shouldAppendBothConditionsWhenStartAndEnd() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            LambdaQueryWrapper<StubEntity> result =
                    QueryHelper.dateBetween(wrapper, StubEntity::getCreateTime, START, END);

            assertSame(wrapper, result, "Should return same wrapper instance");
            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have conditions when both start and end are present");
            // Both >= start and <= end should be in the segment
            assertTrue(wrapper.getSqlSegment().contains(">=") || wrapper.getSqlSegment().contains("GE"),
                    "Should contain >= condition for start");
            assertTrue(wrapper.getSqlSegment().contains("<=") || wrapper.getSqlSegment().contains("LE"),
                    "Should contain <= condition for end");
        }

        @Test
        @DisplayName("should append only ge condition when only start is present")
        void shouldAppendOnlyGeWhenOnlyStartPresent() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.dateBetween(wrapper, StubEntity::getCreateTime, START, null);

            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have a condition when start is present");
            assertTrue(wrapper.getSqlSegment().contains(">=") || wrapper.getSqlSegment().contains("GE"),
                    "Should contain >= condition");
        }

        @Test
        @DisplayName("should append only le condition when only end is present")
        void shouldAppendOnlyLeWhenOnlyEndPresent() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.dateBetween(wrapper, StubEntity::getCreateTime, null, END);

            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have a condition when end is present");
            assertTrue(wrapper.getSqlSegment().contains("<=") || wrapper.getSqlSegment().contains("LE"),
                    "Should contain <= condition");
        }

        @Test
        @DisplayName("should not append any conditions when both start and end are null")
        void shouldNotAppendConditionsWhenBothNull() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.dateBetween(wrapper, StubEntity::getCreateTime, null, null);

            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when both start and end are null");
        }
    }

    // ==================== conditionEq ====================

    @Nested
    @DisplayName("conditionEq")
    class ConditionEq {

        @Test
        @DisplayName("should append eq condition when condition is true")
        void shouldAppendConditionWhenConditionIsTrue() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            LambdaQueryWrapper<StubEntity> result =
                    QueryHelper.conditionEq(wrapper, true, StubEntity::getStatus, 1);

            assertSame(wrapper, result, "Should return same wrapper instance");
            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have SQL segment when condition is true");
        }

        @Test
        @DisplayName("should not append eq condition when condition is false")
        void shouldNotAppendConditionWhenConditionIsFalse() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            LambdaQueryWrapper<StubEntity> result =
                    QueryHelper.conditionEq(wrapper, false, StubEntity::getStatus, 1);

            assertSame(wrapper, result, "Should return same wrapper instance");
            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when condition is false");
        }
    }

    // ==================== conditionEqIfPresent ====================

    @Nested
    @DisplayName("conditionEqIfPresent")
    class ConditionEqIfPresent {

        @Test
        @DisplayName("should append eq condition when value is non-null")
        void shouldAppendConditionWhenValueIsNonNull() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.conditionEqIfPresent(wrapper, StubEntity::getStatus, 1);

            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have SQL segment when value is non-null");
        }

        @Test
        @DisplayName("should not append eq condition when value is null")
        void shouldNotAppendConditionWhenValueIsNull() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.conditionEqIfPresent(wrapper, StubEntity::getStatus, null);

            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when value is null");
        }
    }

    // ==================== conditionIn ====================

    @Nested
    @DisplayName("conditionIn")
    class ConditionIn {

        @Test
        @DisplayName("should append in condition when values collection is non-empty")
        void shouldAppendConditionWhenValuesNonEmpty() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.conditionIn(wrapper, StubEntity::getId, Arrays.asList(1L, 2L, 3L));

            assertFalse(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have SQL segment when values are present");
        }

        @Test
        @DisplayName("should not append in condition when values collection is empty")
        void shouldNotAppendConditionWhenValuesEmpty() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.conditionIn(wrapper, StubEntity::getId, Collections.emptyList());

            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when values list is empty");
        }

        @Test
        @DisplayName("should not append in condition when values collection is null")
        void shouldNotAppendConditionWhenValuesNull() {
            LambdaQueryWrapper<StubEntity> wrapper = new LambdaQueryWrapper<>();
            QueryHelper.conditionIn(wrapper, StubEntity::getId, null);

            assertTrue(wrapper.getSqlSegment() == null || wrapper.getSqlSegment().isBlank(),
                    "Wrapper should have no conditions when values list is null");
        }
    }

    // ==================== constructor ====================

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should throw UnsupportedOperationException when instantiated via reflection")
        void shouldThrowWhenInstantiated() throws Exception {
            var constructor = QueryHelper.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThrows(Exception.class, constructor::newInstance,
                    "Utility class should not be instantiatable");
        }
    }
}
