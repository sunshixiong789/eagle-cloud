package com.eagle.zhetaoke.jd;

import com.eagle.zhetaoke.dto.*;
import com.eagle.zhetaoke.jd.client.ZhejingkeApiService;
import com.eagle.zhetaoke.jd.client.ZhejingkeClient;
import com.eagle.zhetaoke.jd.dto.JdOrderDetail;
import com.eagle.zhetaoke.jd.properties.ZhejingkeProperties;
import com.eagle.zhetaoke.jd.request.*;
import com.eagle.zhetaoke.request.LinkConvertRequest;
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
 * {@link ZhejingkeApiService} 单元测试。
 *
 * @author 孙士雄
 */
class ZhejingkeApiServiceTest {

    @Mock
    private ZhejingkeClient client;

    @Mock
    private ZhejingkeProperties properties;

    @InjectMocks
    private ZhejingkeApiService apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getAppkey()).thenReturn("test-appkey");
    }

    // ==================== 商品查询（简单版本）====================

    @Test
    void shouldGetAllItemsWithSimpleParams() {
        ZhetaokeItem item = new ZhetaokeItem();
        item.setTaoId("123");
        item.setTitle("商品1");

        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of(item));

        when(client.getAllItems(eq("test-appkey"), eq(1), eq(20), isNull(), isNull()))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getAllItems(1, 20);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("商品1");
    }

    @Test
    void shouldGetAllItemsWithRequestObject() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getAllItems(eq("test-appkey"), eq(2), eq(50), eq("price_desc"), eq(1)))
                .thenReturn(mockResponse);

        JdItemFilterRequest request = new JdItemFilterRequest();
        request.setPage(2);
        request.setPageSize(50);
        request.setSort("price_desc");
        request.setCid(1);

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getAllItems(request);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldUseRequestAppkeyOverProperties() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getAllItems(eq("custom-key"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        JdItemFilterRequest request = new JdItemFilterRequest();
        request.setAppkey("custom-key");

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getAllItems(request);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldSearchItemsWithSimpleParams() {
        ZhetaokeItem item = new ZhetaokeItem();
        item.setTaoId("456");
        item.setTitle("手机壳");

        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of(item));

        when(client.searchItems(eq("test-appkey"), eq(1), eq(20), eq("new"), eq("手机")))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.searchItems("手机");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("手机壳");
    }

    @Test
    void shouldSearchItemsWithRequestObject() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.searchItems(eq("test-appkey"), isNull(), eq(50), eq("sale_num_desc"), eq("电脑")))
                .thenReturn(mockResponse);

        JdSearchRequest request = new JdSearchRequest();
        request.setQ("电脑");
        request.setPageSize(50);
        request.setSort("sale_num_desc");

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.searchItems(request);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldGetItemDetail() {
        ZhetaokeItem item = new ZhetaokeItem();
        item.setTaoId("789");
        item.setTitle("详情商品");

        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of(item));

        when(client.getItemDetail(eq("test-appkey"), eq("789")))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getItemDetail("789");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().get(0).getTaoId()).isEqualTo("789");
    }

    // ==================== 销量榜 ====================

    @Test
    void shouldGetHourlyRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getHourlyRank(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getHourlyRank(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetDailyRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getDailyRank(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getDailyRank(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetRealTimeRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getRealTimeRank(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getRealTimeRank(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetCommissionRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getCommissionRank(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getCommissionRank(1, 20).isSuccess()).isTrue();
    }

    // ==================== 价格商品 ====================

    @Test
    void shouldGetNineItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getNineItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getNineItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetNineteenItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getNineteenItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getNineteenItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetXianbaoItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getXianbaoItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getXianbaoItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetDongdongItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getDongdongItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getDongdongItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetTodayItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getTodayItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getTodayItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetYugaoItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getYugaoItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getYugaoItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGaoyongjinItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGaoyongjinItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGaoyongjinItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGaoxiaoliangItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGaoxiaoliangItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGaoxiaoliangItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGaopingfenItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGaopingfenItems(any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGaopingfenItems(1, 20).isSuccess()).isTrue();
    }

    // ==================== 辅助工具 API ====================

    @Test
    void shouldGetHotWords() {
        ZhetaokeHotWord word = new ZhetaokeHotWord();
        word.setWord("手机");

        ZhetaokeResponse<List<ZhetaokeHotWord>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of(word));

        when(client.getHotWords(eq("test-appkey"))).thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeHotWord>> result = apiService.getHotWords();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().get(0).getWord()).isEqualTo("手机");
    }

    @Test
    void shouldGetSuggestWords() {
        ZhetaokeSuggestWord word = new ZhetaokeSuggestWord();
        word.setWord("苹果手机");

        ZhetaokeResponse<List<ZhetaokeSuggestWord>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of(word));

        when(client.getSuggestWords(eq("test-appkey"), eq("苹果"))).thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeSuggestWord>> result = apiService.getSuggestWords("苹果");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().get(0).getWord()).isEqualTo("苹果手机");
    }

    // ==================== 转链 ====================

    @Test
    void shouldConvertJdLinkWithSimpleParams() {
        ZhetaokeLinkResult linkResult = new ZhetaokeLinkResult();
        linkResult.setStatus(200);
        linkResult.setShortUrl("https://u.jd.com/abc");

        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(linkResult);

        when(client.convertJdLink(eq("test-appkey"), eq("https://item.jd.com/123.html"),
                eq("union-123"), isNull(), eq("2")))
                .thenReturn(mockResponse);

        ZhetaokeResponse<ZhetaokeLinkResult> result = apiService.convertJdLink("https://item.jd.com/123.html", "union-123");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().getShortUrl()).isEqualTo("https://u.jd.com/abc");
    }

    @Test
    void shouldConvertJdLinkWithRequestObject() {
        ZhetaokeLinkResult linkResult = new ZhetaokeLinkResult();
        linkResult.setStatus(200);

        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(linkResult);

        when(client.convertJdLink(eq("test-appkey"), eq("url"), eq("union"), eq("pos"), eq("3")))
                .thenReturn(mockResponse);

        LinkConvertRequest request = new LinkConvertRequest();
        request.setMaterialId("url");
        request.setUnionId("union");
        request.setPositionId("pos");
        request.setChainType("3");

        ZhetaokeResponse<ZhetaokeLinkResult> result = apiService.convertJdLink(request);

        assertThat(result.isSuccess()).isTrue();
    }

    // ==================== 订单查询 ====================

    @Test
    void shouldQueryJdOrdersWithSimpleParams() {
        JdOrderDetail order = new JdOrderDetail();
        order.setOrderId("order-123");
        order.setSkuName("京东商品");

        ZhetaokeResponse<List<JdOrderDetail>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of(order));

        when(client.queryJdOrders(eq("test-appkey"), eq("union-1"), eq("2024-01-01"), eq("2024-01-31"), eq(1), eq(20)))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<JdOrderDetail>> result = apiService.queryJdOrders("union-1", "2024-01-01", "2024-01-31", 1, 20);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo("order-123");
    }

    @Test
    void shouldQueryJdOrdersWithRequestObject() {
        ZhetaokeResponse<List<JdOrderDetail>> mockResponse = createSuccessResponse(List.of());

        when(client.queryJdOrders(eq("test-appkey"), eq("union-1"), eq("2024-01-01"), eq("2024-01-31"), isNull(), eq(50)))
                .thenReturn(mockResponse);

        JdOrderQueryRequest request = new JdOrderQueryRequest();
        request.setUnionId("union-1");
        request.setStartTime("2024-01-01");
        request.setEndTime("2024-01-31");
        request.setPageSize(50);

        assertThat(apiService.queryJdOrders(request).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryJdOrders2() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());

        when(client.queryJdOrders2(eq("test-appkey"), eq("union-1"), eq("2024-01-01"), eq("2024-01-31"), eq(1), eq(20)))
                .thenReturn(mockResponse);

        assertThat(apiService.queryJdOrders2("union-1", "2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    // ==================== 推广位管理 ====================

    @Test
    void shouldCreateJdPosition() {
        ZhetaokeResponse<String> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent("position-123");

        when(client.createJdPosition(eq("test-appkey"), eq("union-1"), eq("新推广位"), isNull()))
                .thenReturn(mockResponse);

        ZhetaokeResponse<String> result = apiService.createJdPosition("union-1", "新推广位");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).isEqualTo("position-123");
    }

    @Test
    void shouldQueryJdPositions() {
        ZhetaokeResponse<List<String>> mockResponse = createSuccessResponse(List.of("pos-1", "pos-2"));

        when(client.queryJdPositions(eq("test-appkey"), eq("union-1"), eq(1), eq(20)))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<String>> result = apiService.queryJdPositions("union-1", 1, 20);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(2);
    }

    // ==================== 辅助方法 ====================

    private <T> ZhetaokeResponse<T> createSuccessResponse(T content) {
        ZhetaokeResponse<T> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent(content);
        return response;
    }
}
