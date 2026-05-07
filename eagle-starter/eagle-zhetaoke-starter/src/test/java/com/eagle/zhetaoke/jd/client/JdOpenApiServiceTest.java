package com.eagle.zhetaoke.jd;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.jd.client.JdOpenApiService;
import com.eagle.zhetaoke.jd.client.JdOpenClient;
import com.eagle.zhetaoke.jd.request.*;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link JdOpenApiService} 单元测试。
 *
 * @author 孙士雄
 */
class JdOpenApiServiceTest {

    @Mock
    private JdOpenClient client;

    @Mock
    private ZhetaokeProperties properties;

    @InjectMocks
    private JdOpenApiService apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getAppkey()).thenReturn("test-appkey");
    }

    @Test
    void shouldConvertJdLinkOldWithSimpleParams() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertJdLinkOld(eq("test-appkey"), eq("https://jd.com"), eq("union-1"), isNull(), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertJdLinkOld("https://jd.com", "union-1").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertJdLinkOldWithRequestObject() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertJdLinkOld(eq("test-appkey"), eq("content"), eq("union"), eq("pos"), eq("coupon")))
                .thenReturn(mockResponse);

        JdLinkConvertRequest request = new JdLinkConvertRequest();
        request.setContent("content");
        request.setJdLianmengId("union");
        request.setPositionId("pos");
        request.setCouponUrl("coupon");

        assertThat(apiService.convertJdLinkOld(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetJdGoodsDetailSimple() {
        ZhetaokeResponse<ZhetaokeItem> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.getJdGoodsDetailSimple(eq("test-appkey"), eq("123")))
                .thenReturn(mockResponse);

        assertThat(apiService.getJdGoodsDetailSimple("123").isSuccess()).isTrue();
    }

    @Test
    void shouldGetJdGoodsBigField() {
        ZhetaokeResponse<ZhetaokeItem> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.getJdGoodsBigField(eq("test-appkey"), eq("123"), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.getJdGoodsBigField("123").isSuccess()).isTrue();
    }

    @Test
    void shouldGetJdGoodsDetailWithRequest() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getJdGoodsDetail(eq("test-appkey"), isNull(), isNull(),
                eq("1"), isNull(), isNull(), eq("1"), eq("20"),
                eq("123"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        JdGoodsDetailRequest request = new JdGoodsDetailRequest();
        request.setCid1("1");
        request.setPageIndex("1");
        request.setPageSize("20");
        request.setSkuIds("123");

        assertThat(apiService.getJdGoodsDetail(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetJingfenGoodsWithSimpleParams() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getJingfenGoods(eq("test-appkey"), eq("1"), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.getJingfenGoods("1").isSuccess()).isTrue();
    }

    @Test
    void shouldGetJingfenGoodsWithRequest() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getJingfenGoods(eq("test-appkey"), eq("1"), eq("1"), eq("20"),
                eq("price"), eq("asc"), eq("pid-1"), eq("fields"), eq("10")))
                .thenReturn(mockResponse);

        JdJingfenRequest request = new JdJingfenRequest();
        request.setEliteId("1");
        request.setPageIndex("1");
        request.setPageSize("20");
        request.setSortName("price");
        request.setSort("asc");
        request.setPid("pid-1");
        request.setFields("fields");
        request.setForbidTypes("10");

        assertThat(apiService.getJingfenGoods(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetJdPyqItemsWithRequest() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getJdPyqItems(eq("20"), isNull(), eq("手机"), isNull(),
                isNull(), isNull(), isNull(), isNull(), eq("new"), isNull()))
                .thenReturn(mockResponse);

        JdPyqItemsRequest request = new JdPyqItemsRequest();
        request.setPageSize("20");
        request.setKeyword("手机");
        request.setSort("new");

        assertThat(apiService.getJdPyqItems(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetJdLiJinItemsWithRequest() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getJdLiJinItems(eq("20"), eq("手机"), isNull(), isNull(),
                isNull(), isNull(), eq("new"), isNull()))
                .thenReturn(mockResponse);

        JdLiJinRequest request = new JdLiJinRequest();
        request.setPageSize("20");
        request.setKeyword("手机");
        request.setSort("new");

        assertThat(apiService.getJdLiJinItems(request).isSuccess()).isTrue();
    }

    private <T> ZhetaokeResponse<T> createSuccessResponse(T content) {
        ZhetaokeResponse<T> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent(content);
        return response;
    }
}
