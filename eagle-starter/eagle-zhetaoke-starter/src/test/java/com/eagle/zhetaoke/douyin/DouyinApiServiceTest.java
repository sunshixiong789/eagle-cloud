package com.eagle.zhetaoke.douyin;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.douyin.client.DouyinApiService;
import com.eagle.zhetaoke.douyin.client.DouyinClient;
import com.eagle.zhetaoke.douyin.request.*;
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
 * {@link DouyinApiService} 单元测试。
 *
 * @author 孙士雄
 */
class DouyinApiServiceTest {

    @Mock
    private DouyinClient client;

    @Mock
    private ZhetaokeProperties properties;

    @InjectMocks
    private DouyinApiService apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getAppkey()).thenReturn("test-appkey");
    }

    @Test
    void shouldConvertDouyinLink2WithSimpleParams() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertDouyinLink2(eq("test-appkey"), eq("sid-1"), eq("url"),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.convertDouyinLink2("sid-1", "url").isSuccess()).isTrue();
    }

    @Test
    void shouldConvertDouyinLink2WithRequest() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertDouyinLink2(eq("test-appkey"), eq("sid-1"), eq("url"),
                eq("ext"), eq("true"), eq("true"), eq("true")))
                .thenReturn(mockResponse);

        DouyinLinkConvertRequest request = new DouyinLinkConvertRequest();
        request.setSid("sid-1");
        request.setProductUrl("url");
        request.setExternalInfo("ext");
        request.setNeedQrCode("true");
        request.setUseCoupon("true");
        request.setNeedShareLink("true");

        assertThat(apiService.convertDouyinLink2(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetDouyinProductDetail() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.getDouyinProductDetail(eq("test-appkey"), eq("sid-1"), eq("123,456"), eq("fields")))
                .thenReturn(mockResponse);

        DouyinProductDetailRequest request = new DouyinProductDetailRequest();
        request.setSid("sid-1");
        request.setProductIds("123,456");
        request.setFields("fields");

        assertThat(apiService.getDouyinProductDetail(request).isSuccess()).isTrue();
    }

    @Test
    void shouldSearchDouyinProducts() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.searchDouyinProducts(eq("test-appkey"), eq("sid-1"), eq("手机"),
                isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                eq("1"), eq("20"), isNull(), isNull()))
                .thenReturn(mockResponse);

        DouyinProductSearchRequest request = new DouyinProductSearchRequest();
        request.setSid("sid-1");
        request.setTitle("手机");
        request.setPage("1");
        request.setPageSize("20");

        assertThat(apiService.searchDouyinProducts(request).isSuccess()).isTrue();
    }

    @Test
    void shouldParseDouyinCommand() {
        ZhetaokeResponse<ZhetaokeItem> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.parseDouyinCommand(eq("test-appkey"), eq("sid-1"), eq("command")))
                .thenReturn(mockResponse);

        assertThat(apiService.parseDouyinCommand("sid-1", "command").isSuccess()).isTrue();
    }

    @Test
    void shouldQueryDouyinOrders2() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());

        when(client.queryDouyinOrders2(eq("test-appkey"), eq("1"), eq("1"), eq("50"),
                eq("2024-01-01"), eq("2024-01-31"), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        DouyinOrderQueryRequest request = new DouyinOrderQueryRequest();
        request.setType("1");
        request.setPage("1");
        request.setPageSize("50");
        request.setStartTime("2024-01-01");
        request.setEndTime("2024-01-31");

        assertThat(apiService.queryDouyinOrders2(request).isSuccess()).isTrue();
    }

    @Test
    void shouldConvertDouyinLiveLink() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertDouyinLiveLink(eq("test-appkey"), eq("sid-1"), eq("buyin"), eq("code"), eq("ext")))
                .thenReturn(mockResponse);

        DouyinLiveLinkRequest request = new DouyinLiveLinkRequest();
        request.setSid("sid-1");
        request.setBuyinId("buyin");
        request.setDyCode("code");
        request.setExternalInfo("ext");

        assertThat(apiService.convertDouyinLiveLink(request).isSuccess()).isTrue();
    }

    private <T> ZhetaokeResponse<T> createSuccessResponse(T content) {
        ZhetaokeResponse<T> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent(content);
        return response;
    }
}
