package com.eagle.zhetaoke;

import com.eagle.zhetaoke.request.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 请求参数对象测试。
 *
 * @author 孙士雄
 */
class ZhetaokeRequestTest {

    // ==================== BaseQueryRequest ====================

    @Test
    void shouldHaveDefaultValuesForBaseQuery() {
        BaseQueryRequest request = new BaseQueryRequest();

        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(20);
        assertThat(request.getSort()).isEqualTo("new");
        assertThat(request.getAppkey()).isNull();
        assertThat(request.getSid()).isNull();
        assertThat(request.getPid()).isNull();
    }

    @Test
    void shouldSetCustomValuesForBaseQuery() {
        BaseQueryRequest request = new BaseQueryRequest();
        request.setAppkey("key");
        request.setSid("sid");
        request.setPid("mm_1_2_3");
        request.setPage(5);
        request.setPageSize(50);
        request.setSort("price_desc");

        assertThat(request.getAppkey()).isEqualTo("key");
        assertThat(request.getSid()).isEqualTo("sid");
        assertThat(request.getPid()).isEqualTo("mm_1_2_3");
        assertThat(request.getPage()).isEqualTo(5);
        assertThat(request.getPageSize()).isEqualTo(50);
        assertThat(request.getSort()).isEqualTo("price_desc");
    }

    // ==================== ItemFilterRequest ====================

    @Test
    void shouldInheritBaseQueryDefaults() {
        ItemFilterRequest request = new ItemFilterRequest();

        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(20);
        assertThat(request.getSort()).isEqualTo("new");
    }

    @Test
    void shouldSetFilterValues() {
        ItemFilterRequest request = new ItemFilterRequest();
        request.setCid(1);
        request.setTj("tmall");
        request.setJt("taoqianggou");
        request.setJh("haitao");
        request.setToday("1");
        request.setYunfeixian("1");
        request.setPinpai("1");
        request.setPrice("0.0-9.9");
        request.setQ("手机");
        request.setBaodan("1");

        assertThat(request.getCid()).isEqualTo(1);
        assertThat(request.getTj()).isEqualTo("tmall");
        assertThat(request.getJt()).isEqualTo("taoqianggou");
        assertThat(request.getJh()).isEqualTo("haitao");
        assertThat(request.getToday()).isEqualTo("1");
        assertThat(request.getYunfeixian()).isEqualTo("1");
        assertThat(request.getPinpai()).isEqualTo("1");
        assertThat(request.getPrice()).isEqualTo("0.0-9.9");
        assertThat(request.getQ()).isEqualTo("手机");
        assertThat(request.getBaodan()).isEqualTo("1");
    }

    @Test
    void shouldHandleNullFilterValues() {
        ItemFilterRequest request = new ItemFilterRequest();

        assertThat(request.getCid()).isNull();
        assertThat(request.getTj()).isNull();
        assertThat(request.getPrice()).isNull();
        assertThat(request.getQ()).isNull();
    }

    // ==================== SearchRequest ====================

    @Test
    void shouldInheritBaseQueryDefaultsForSearch() {
        SearchRequest request = new SearchRequest();

        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(20);
    }

    @Test
    void shouldSetSearchValues() {
        SearchRequest request = new SearchRequest();
        request.setQ("笔记本电脑");
        request.setMaterialId("12345");
        request.setYouquan("1");
        request.setHaiwai("1");
        request.setHaoping("1");

        assertThat(request.getQ()).isEqualTo("笔记本电脑");
        assertThat(request.getMaterialId()).isEqualTo("12345");
        assertThat(request.getYouquan()).isEqualTo("1");
        assertThat(request.getHaiwai()).isEqualTo("1");
        assertThat(request.getHaoping()).isEqualTo("1");
    }

    // ==================== ItemDetailRequest ====================

    @Test
    void shouldSetDetailValues() {
        ItemDetailRequest request = new ItemDetailRequest();
        request.setAppkey("key");
        request.setTaoId("123456");
        request.setCode("code-1");
        request.setNumIids("1,2,3");
        request.setType("0");

        assertThat(request.getAppkey()).isEqualTo("key");
        assertThat(request.getTaoId()).isEqualTo("123456");
        assertThat(request.getCode()).isEqualTo("code-1");
        assertThat(request.getNumIids()).isEqualTo("1,2,3");
        assertThat(request.getType()).isEqualTo("0");
    }

    @Test
    void shouldHaveNullDefaultsForDetail() {
        ItemDetailRequest request = new ItemDetailRequest();

        assertThat(request.getTaoId()).isNull();
        assertThat(request.getCode()).isNull();
        assertThat(request.getNumIids()).isNull();
    }

    // ==================== LinkConvertRequest ====================

    @Test
    void shouldHaveDefaultChainType() {
        LinkConvertRequest request = new LinkConvertRequest();

        assertThat(request.getChainType()).isEqualTo("2");
    }

    @Test
    void shouldSetConvertValues() {
        LinkConvertRequest request = new LinkConvertRequest();
        request.setAppkey("key");
        request.setLink("https://example.com");
        request.setMaterialId("material-1");
        request.setUnionId("union-123");
        request.setPositionId("pos-1");
        request.setChainType("3");
        request.setPlatform("ios");
        request.setPid("mm_1_2_3");
        request.setCouponUrl("https://coupon.com");

        assertThat(request.getLink()).isEqualTo("https://example.com");
        assertThat(request.getMaterialId()).isEqualTo("material-1");
        assertThat(request.getUnionId()).isEqualTo("union-123");
        assertThat(request.getPositionId()).isEqualTo("pos-1");
        assertThat(request.getChainType()).isEqualTo("3");
        assertThat(request.getPlatform()).isEqualTo("ios");
        assertThat(request.getCouponUrl()).isEqualTo("https://coupon.com");
    }

    // ==================== OrderQueryRequest ====================

    @Test
    void shouldHaveDefaultValuesForOrderQuery() {
        OrderQueryRequest request = new OrderQueryRequest();

        assertThat(request.getPage()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(20);
        assertThat(request.getUnionId()).isNull();
        assertThat(request.getStartTime()).isNull();
    }

    @Test
    void shouldSetOrderQueryValues() {
        OrderQueryRequest request = new OrderQueryRequest();
        request.setAppkey("key");
        request.setUnionId("union-1");
        request.setStartTime("2024-01-01 00:00:00");
        request.setEndTime("2024-01-31 23:59:59");
        request.setPage(2);
        request.setPageSize(50);
        request.setPlatform("jd");

        assertThat(request.getUnionId()).isEqualTo("union-1");
        assertThat(request.getStartTime()).isEqualTo("2024-01-01 00:00:00");
        assertThat(request.getEndTime()).isEqualTo("2024-01-31 23:59:59");
        assertThat(request.getPage()).isEqualTo(2);
        assertThat(request.getPageSize()).isEqualTo(50);
        assertThat(request.getPlatform()).isEqualTo("jd");
    }

    // ==================== 继承关系测试 ====================

    @Test
    void shouldBeInstanceOfBaseQueryRequest() {
        ItemFilterRequest filterRequest = new ItemFilterRequest();
        SearchRequest searchRequest = new SearchRequest();

        assertThat(filterRequest).isInstanceOf(BaseQueryRequest.class);
        assertThat(searchRequest).isInstanceOf(BaseQueryRequest.class);
    }
}
