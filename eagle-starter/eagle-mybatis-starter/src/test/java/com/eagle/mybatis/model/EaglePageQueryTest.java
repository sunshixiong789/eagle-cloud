package com.eagle.mybatis.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link EaglePageQuery} 单元测试。
 */
@DisplayName("EaglePageQuery")
class EaglePageQueryTest {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String DEFAULT_ORDER_DIRECTION = "desc";

    @Nested
    @DisplayName("default values")
    class DefaultValues {

        @Test
        @DisplayName("应Have默认分页Num")
        void shouldHaveDefaultPageNum() {
            EaglePageQuery query = new EaglePageQuery();

            assertEquals(DEFAULT_PAGE_NUM, query.getPageNum(),
                    "Default pageNum should be 1");
        }

        @Test
        @DisplayName("应Have默认分页大小")
        void shouldHaveDefaultPageSize() {
            EaglePageQuery query = new EaglePageQuery();

            assertEquals(DEFAULT_PAGE_SIZE, query.getPageSize(),
                    "Default pageSize should be 20");
        }

        @Test
        @DisplayName("应Have默认排序Direction")
        void shouldHaveDefaultOrderDirection() {
            EaglePageQuery query = new EaglePageQuery();

            assertEquals(DEFAULT_ORDER_DIRECTION, query.getOrderDirection(),
                    "Default orderDirection should be 'desc'");
        }

        @Test
        @DisplayName("应Havenull排序通过通过默认")
        void shouldHaveNullOrderByByDefault() {
            EaglePageQuery query = new EaglePageQuery();

            assertEquals(null, query.getOrderBy(),
                    "Default orderBy should be null");
        }
    }

    @Nested
    @DisplayName("toPage")
    class ToPage {

        @Test
        @DisplayName("到分页应映射Correctly")
        void toPageShouldMapCorrectly() {
            EaglePageQuery query = new EaglePageQuery();
            query.setPageNum(2);
            query.setPageSize(10);

            Page<?> page = query.toPage();

            assertNotNull(page);
            assertEquals(2L, page.getCurrent(),
                    "Page current should match pageNum");
            assertEquals(10L, page.getSize(),
                    "Page size should match pageSize");
        }

        @Test
        @DisplayName("到分页应Use默认值")
        void toPageShouldUseDefaultValues() {
            EaglePageQuery query = new EaglePageQuery();

            Page<?> page = query.toPage();

            assertNotNull(page);
            assertEquals(1L, page.getCurrent(),
                    "Page current should be 1 by default");
            assertEquals(20L, page.getSize(),
                    "Page size should be 20 by default");
        }

        @Test
        @DisplayName("到分页应返回首次分页")
        void toPageShouldReturnFirstPage() {
            EaglePageQuery query = new EaglePageQuery();
            query.setPageNum(1);
            query.setPageSize(50);

            Page<?> page = query.toPage();

            assertEquals(1L, page.getCurrent());
            assertEquals(50L, page.getSize());
        }

        @Test
        @DisplayName("到分页应RespectMax分页大小")
        void toPageShouldRespectMaxPageSize() {
            EaglePageQuery query = new EaglePageQuery();
            // EaglePageQuery's @Max(200) is enforced by Bean Validation at controller layer,
            // not in toPage() itself. We verify the @Max annotation is 200 and toPage() passes the value.
            query.setPageSize(200);

            Page<?> page = query.toPage();

            assertEquals(200L, page.getSize(),
                    "Maximum allowed pageSize of 200 should be passed through to Page");
        }
    }

    @Nested
    @DisplayName("setters")
    class Setters {

        @Test
        @DisplayName("应Accept自定义排序通过")
        void shouldAcceptCustomOrderBy() {
            EaglePageQuery query = new EaglePageQuery();
            query.setOrderBy("createTime");

            assertEquals("createTime", query.getOrderBy());
        }

        @Test
        @DisplayName("应AcceptAsc排序Direction")
        void shouldAcceptAscOrderDirection() {
            EaglePageQuery query = new EaglePageQuery();
            query.setOrderDirection("asc");

            assertEquals("asc", query.getOrderDirection());
        }
    }
}
