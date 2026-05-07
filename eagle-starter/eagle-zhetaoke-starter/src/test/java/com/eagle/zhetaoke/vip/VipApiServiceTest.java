package com.eagle.zhetaoke.vip;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.properties.ZhetaokeProperties;
import com.eagle.zhetaoke.vip.client.VipApiService;
import com.eagle.zhetaoke.vip.client.VipClient;
import com.eagle.zhetaoke.vip.request.*;
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
 * {@link VipApiService} 单元测试。
 *
 * @author 孙士雄
 */
class VipApiServiceTest {

    @Mock
    private VipClient client;

    @Mock
    private ZhetaokeProperties properties;

    @InjectMocks
    private VipApiService apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getAppkey()).thenReturn("test-appkey");
    }

    @Test
    void shouldConvertVipLinkWithSimpleParams() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertVipLink(eq("test-appkey"), eq("sid-1"), eq("url"), isNull(), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertVipLink("sid-1", "url").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertVipLinkWithRequest() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertVipLink(eq("test-appkey"), eq("sid-1"), eq("url"), eq("tag"), eq("stat")))
                .thenReturn(mockResponse);

        VipLinkConvertRequest request = new VipLinkConvertRequest();
        request.setSid("sid-1");
        request.setUrl("url");
        request.setChanTag("tag");
        request.setStatParam("stat");

        assertThat(apiService.convertVipLink(request).isSuccess()).isTrue();
    }

    @Test
    void shouldAuthorizeVipAccount() {
        ZhetaokeResponse<String> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.authorizeVipAccount(eq("test-appkey"))).thenReturn(mockResponse);

        assertThat(apiService.authorizeVipAccount().isSuccess()).isTrue();
    }

    @Test
    void shouldGetVipAuthorizationList() {
        ZhetaokeResponse<List<String>> mockResponse = createSuccessResponse(List.of());

        when(client.getVipAuthorizationList(eq("test-appkey"), eq("1"), eq("3"), eq("sid-1")))
                .thenReturn(mockResponse);

        VipAuthListRequest request = new VipAuthListRequest();
        request.setPage("1");
        request.setExpireDay("3");
        request.setSid("sid-1");

        assertThat(apiService.getVipAuthorizationList(request).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryVipOrders2() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());

        when(client.queryVipOrders2(eq("test-appkey"), eq("sid-1"), isNull(),
                eq("1000"), eq("2000"), eq("1"), eq("20"),
                isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        VipOrderQueryRequest request = new VipOrderQueryRequest();
        request.setSid("sid-1");
        request.setOrderTimeStart("1000");
        request.setOrderTimeEnd("2000");
        request.setPage("1");
        request.setPageSize("20");

        assertThat(apiService.queryVipOrders2(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetVipGoodsDetail() {
        ZhetaokeResponse<ZhetaokeItem> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.getVipGoodsDetail(eq("test-appkey"), eq("sid-1"), eq("123")))
                .thenReturn(mockResponse);

        assertThat(apiService.getVipGoodsDetail("sid-1", "123").isSuccess()).isTrue();
    }

    @Test
    void shouldGetVipWenanList() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getVipWenanList(eq("test-appkey"), eq("1"), eq("20"), eq("new"), eq("1")))
                .thenReturn(mockResponse);

        VipWenanRequest request = new VipWenanRequest();
        request.setPage("1");
        request.setPageSize("20");
        request.setSort("new");
        request.setTotalCount("1");

        assertThat(apiService.getVipWenanList(request).isSuccess()).isTrue();
    }

    @Test
    void shouldSearchVipGoods() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.searchVipGoods(eq("test-appkey"), eq("sid-1"), eq("手机"),
                eq("PRICE"), eq("0"), eq("1"), eq("20"), eq("0"), eq("1000")))
                .thenReturn(mockResponse);

        VipSearchRequest request = new VipSearchRequest();
        request.setSid("sid-1");
        request.setKeyword("手机");
        request.setFieldName("PRICE");
        request.setOrder("0");
        request.setPage("1");
        request.setPageSize("20");
        request.setPriceStart("0");
        request.setPriceEnd("1000");

        assertThat(apiService.searchVipGoods(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetVipGoodsDetailV2() {
        ZhetaokeResponse<ZhetaokeItem> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.getVipGoodsDetailV2(eq("test-appkey"), eq("sid-1"), eq("123"),
                eq("true"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        VipGoodsDetailV2Request request = new VipGoodsDetailV2Request();
        request.setSid("sid-1");
        request.setId("123");
        request.setQueryDetail("true");

        assertThat(apiService.getVipGoodsDetailV2(request).isSuccess()).isTrue();
    }

    private <T> ZhetaokeResponse<T> createSuccessResponse(T content) {
        ZhetaokeResponse<T> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent(content);
        return response;
    }
}
