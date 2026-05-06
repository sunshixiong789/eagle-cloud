package com.eagle.zhetaoke;

import com.eagle.zhetaoke.client.ZhetaokeApiService;
import com.eagle.zhetaoke.client.ZhetaokeClient;
import com.eagle.zhetaoke.dto.*;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import com.eagle.zhetaoke.request.*;
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
 * {@link ZhetaokeApiService} 单元测试。
 *
 * @author 孙士雄
 */
class ZhetaokeApiServiceTest {

    @Mock
    private ZhetaokeClient client;

    @Mock
    private ZhetaokeProperties properties;

    @InjectMocks
    private ZhetaokeApiService apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getAppkey()).thenReturn("test-appkey");
        when(properties.getSid()).thenReturn("test-sid");
        when(properties.getPid()).thenReturn("mm_1_2_3");
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

        when(client.getAllItems(eq("test-appkey"), eq("test-sid"), eq("mm_1_2_3"),
                eq(1), eq(20), eq("new"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getAllItems(1, 20, "new");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("商品1");
    }

    @Test
    void shouldGetAllItemsWithRequestObject() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getAllItems(eq("test-appkey"), eq("test-sid"), eq("mm_1_2_3"),
                eq(2), eq(50), eq("price_desc"), eq(1), eq("tmall"), isNull(), isNull()))
                .thenReturn(mockResponse);

        ItemFilterRequest request = new ItemFilterRequest();
        request.setPage(2);
        request.setPageSize(50);
        request.setSort("price_desc");
        request.setCid(1);
        request.setTj("tmall");

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getAllItems(request);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldUseRequestAppkeyOverProperties() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getAllItems(eq("custom-key"), eq("custom-sid"), eq("custom-pid"),
                eq(1), eq(20), eq("new"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        ItemFilterRequest request = new ItemFilterRequest();
        request.setAppkey("custom-key");
        request.setSid("custom-sid");
        request.setPid("custom-pid");

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

        when(client.searchItems(eq("test-appkey"), eq("test-sid"), eq("mm_1_2_3"),
                eq(1), eq(20), eq("new"), eq("手机")))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.searchItems("手机");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("手机壳");
    }

    @Test
    void shouldSearchItemsWithRequestObject() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.searchItems(eq("test-appkey"), eq("test-sid"), eq("mm_1_2_3"),
                eq(1), eq(50), eq("sale_num_desc"), eq("电脑")))
                .thenReturn(mockResponse);

        SearchRequest request = new SearchRequest();
        request.setQ("电脑");
        request.setPageSize(50);
        request.setSort("sale_num_desc");

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.searchItems(request);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void shouldGetItemDetailWithSimpleParams() {
        ZhetaokeItem item = new ZhetaokeItem();
        item.setTaoId("789");
        item.setTitle("详情商品");

        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of(item));

        when(client.getItemDetail(eq("test-appkey"), eq("test-sid"), eq("mm_1_2_3"),
                eq("789"), isNull(), isNull()))
                .thenReturn(mockResponse);

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getItemDetail("789");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent().get(0).getTaoId()).isEqualTo("789");
    }

    @Test
    void shouldGetItemDetailWithRequestObject() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getItemDetail(eq("test-appkey"), eq("test-sid"), eq("mm_1_2_3"),
                eq("999"), eq("code-123"), isNull()))
                .thenReturn(mockResponse);

        ItemDetailRequest request = new ItemDetailRequest();
        request.setTaoId("999");
        request.setCode("code-123");

        ZhetaokeResponse<List<ZhetaokeItem>> result = apiService.getItemDetail(request);

        assertThat(result.isSuccess()).isTrue();
    }

    // ==================== 销量榜 ====================

    @Test
    void shouldGetHourlyRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getHourlyRank(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getHourlyRank(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetDailyRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getDailyRank(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getDailyRank(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetRealTimeRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getRealTimeRank(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getRealTimeRank(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetCommissionRank() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getCommissionRank(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getCommissionRank(1, 20).isSuccess()).isTrue();
    }

    // ==================== 价格/分类商品 ====================

    @Test
    void shouldGetNineItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getNineItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getNineItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetNineteenItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getNineteenItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getNineteenItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetVideoItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getVideoItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getVideoItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetXianbaoItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getXianbaoItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getXianbaoItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetDongdongItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getDongdongItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getDongdongItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetPyqItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getPyqItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getPyqItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetTmallItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getTmallItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getTmallItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGoldItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGoldItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGoldItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetTaoqianggouItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getTaoqianggouItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getTaoqianggouItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetJuhuasuanItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getJuhuasuanItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getJuhuasuanItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetHaitaoItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getHaitaoItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getHaitaoItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetJiyoujiaItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getJiyoujiaItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getJiyoujiaItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetTodayItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getTodayItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getTodayItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetPinpaiItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getPinpaiItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getPinpaiItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetTmallChaoshiItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getTmallChaoshiItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getTmallChaoshiItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetYugaoItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getYugaoItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getYugaoItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetDianpuItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getDianpuItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getDianpuItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGaoyongjinItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGaoyongjinItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGaoyongjinItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGaoxiaoliangItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGaoxiaoliangItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGaoxiaoliangItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGaopingfenItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGaopingfenItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGaopingfenItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetGaomianeItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGaomianeItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getGaomianeItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetPybaoyouItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getPybaoyouItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getPybaoyouItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetBaodanItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getBaodanItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getBaodanItems(1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldGetShixiaoItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getShixiaoItems(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.getShixiaoItems(1, 20).isSuccess()).isTrue();
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

    @Test
    void shouldGetLunboItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getLunboItems(eq("test-appkey"))).thenReturn(mockResponse);

        assertThat(apiService.getLunboItems().isSuccess()).isTrue();
    }

    @Test
    void shouldGetGiftItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGiftItems(eq("test-appkey"))).thenReturn(mockResponse);

        assertThat(apiService.getGiftItems().isSuccess()).isTrue();
    }

    @Test
    void shouldGetFenci() {
        ZhetaokeResponse<List<String>> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);
        mockResponse.setContent(List.of("手机", "壳"));

        when(client.getFenci(eq("test-appkey"), eq("手机壳"))).thenReturn(mockResponse);

        ZhetaokeResponse<List<String>> result = apiService.getFenci("手机壳");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).containsExactly("手机", "壳");
    }

    @Test
    void shouldGetGuessLikeItems() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());
        when(client.getGuessLikeItems(eq("test-appkey"), eq("123"))).thenReturn(mockResponse);

        assertThat(apiService.getGuessLikeItems("123").isSuccess()).isTrue();
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

    @Test
    void shouldConvertMeituanLinkWithSimpleParams() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertMeituanLink(eq("test-appkey"), eq("https://mt.com"), isNull(), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertMeituanLink("https://mt.com").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertMeituanLinkWithRequestObject() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertMeituanLink(eq("test-appkey"), eq("https://mt.com"), eq("pid-1"), eq("ios")))
                .thenReturn(mockResponse);

        LinkConvertRequest request = new LinkConvertRequest();
        request.setLink("https://mt.com");
        request.setPid("pid-1");
        request.setPlatform("ios");

        assertThat(apiService.convertMeituanLink(request).isSuccess()).isTrue();
    }

    @Test
    void shouldConvertElemeLink() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertElemeLink(eq("test-appkey"), eq("https://ele.me"), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertElemeLink("https://ele.me").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertVipLink() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertVipLink(eq("test-appkey"), eq("https://vip.com"), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertVipLink("https://vip.com").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertKaolaLink() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertKaolaLink(eq("test-appkey"), eq("https://kaola.com"), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertKaolaLink("https://kaola.com").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertPddLink() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertPddLink(eq("test-appkey"), eq("https://pdd.com"), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertPddLink("https://pdd.com").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertDouyinLink() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertDouyinLink(eq("test-appkey"), eq("https://douyin.com"), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertDouyinLink("https://douyin.com").isSuccess()).isTrue();
    }

    // ==================== 订单查询 ====================

    @Test
    void shouldQueryJdOrdersWithSimpleParams() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryJdOrders(eq("test-appkey"), eq("union-1"), eq("2024-01-01"), eq("2024-01-31"), eq(1), eq(20)))
                .thenReturn(mockResponse);

        assertThat(apiService.queryJdOrders("union-1", "2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryJdOrdersWithRequestObject() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryJdOrders(eq("test-appkey"), eq("union-1"), eq("2024-01-01"), eq("2024-01-31"), eq(1), eq(50)))
                .thenReturn(mockResponse);

        OrderQueryRequest request = new OrderQueryRequest();
        request.setUnionId("union-1");
        request.setStartTime("2024-01-01");
        request.setEndTime("2024-01-31");
        request.setPageSize(50);

        assertThat(apiService.queryJdOrders(request).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryMeituanOrders() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryMeituanOrders(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.queryMeituanOrders("2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryElemeOrders() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryElemeOrders(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.queryElemeOrders("2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryVipOrders() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryVipOrders(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.queryVipOrders("2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryKaolaOrders() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryKaolaOrders(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.queryKaolaOrders("2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryPddOrders() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryPddOrders(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.queryPddOrders("2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryDouyinOrders() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());
        when(client.queryDouyinOrders(any(), any(), any(), any(), any())).thenReturn(mockResponse);

        assertThat(apiService.queryDouyinOrders("2024-01-01", "2024-01-31", 1, 20).isSuccess()).isTrue();
    }

    // ==================== 辅助方法 ====================

    private <T> ZhetaokeResponse<T> createSuccessResponse(T content) {
        ZhetaokeResponse<T> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent(content);
        return response;
    }
}
