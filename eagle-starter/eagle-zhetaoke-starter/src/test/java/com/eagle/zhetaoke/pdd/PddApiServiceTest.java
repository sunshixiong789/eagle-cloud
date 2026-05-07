package com.eagle.zhetaoke.pdd;

import com.eagle.zhetaoke.dto.ZhetaokeItem;
import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.pdd.client.PddApiService;
import com.eagle.zhetaoke.pdd.client.PddClient;
import com.eagle.zhetaoke.pdd.request.*;
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
 * {@link PddApiService} 单元测试。
 *
 * @author 孙士雄
 */
class PddApiServiceTest {

    @Mock
    private PddClient client;

    @Mock
    private ZhetaokeProperties properties;

    @InjectMocks
    private PddApiService apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getAppkey()).thenReturn("test-appkey");
    }

    @Test
    void shouldConvertPddLink2() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.convertPddLink2(eq("test-appkey"), eq("pdd-key"), eq("pdd-secret"),
                eq("pid-1"), eq("123"), isNull()))
                .thenReturn(mockResponse);

        PddLinkConvertRequest request = new PddLinkConvertRequest();
        request.setPddAppKey("pdd-key");
        request.setPddAppSecret("pdd-secret");
        request.setPid("pid-1");
        request.setContent("123");

        assertThat(apiService.convertPddLink2(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetPddGoodsDetailSimple() {
        ZhetaokeResponse<ZhetaokeItem> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.getPddGoodsDetailSimple(eq("test-appkey"), eq("pdd-key"), eq("pdd-secret"),
                eq("pid-1"), eq("123")))
                .thenReturn(mockResponse);

        PddGoodsDetailRequest request = new PddGoodsDetailRequest();
        request.setPddAppKey("pdd-key");
        request.setPddAppSecret("pdd-secret");
        request.setPid("pid-1");
        request.setContent("123");

        assertThat(apiService.getPddGoodsDetailSimple(request).isSuccess()).isTrue();
    }

    @Test
    void shouldAuthorizePddAccount() {
        ZhetaokeResponse<String> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.authorizePddAccount(eq("test-appkey"), eq("pdd-key"), eq("pdd-secret"),
                eq("pid-1"), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        PddAuthRequest request = new PddAuthRequest();
        request.setPddAppKey("pdd-key");
        request.setPddAppSecret("pdd-secret");
        request.setPid("pid-1");

        assertThat(apiService.authorizePddAccount(request).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryPddAuthStatus() {
        ZhetaokeResponse<String> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.queryPddAuthStatus(eq("test-appkey"), eq("pdd-key"), eq("pdd-secret"),
                eq("pid-1"), isNull()))
                .thenReturn(mockResponse);

        PddAuthQueryRequest request = new PddAuthQueryRequest();
        request.setPddAppKey("pdd-key");
        request.setPddAppSecret("pdd-secret");
        request.setPid("pid-1");

        assertThat(apiService.queryPddAuthStatus(request).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryPddOrders2() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());

        when(client.queryPddOrders2(eq("test-appkey"), eq("pdd-key"), eq("pdd-secret"),
                eq("1000"), eq("2000"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        PddOrderQueryRequest request = new PddOrderQueryRequest();
        request.setPddAppKey("pdd-key");
        request.setPddAppSecret("pdd-secret");
        request.setStartUpdateTime("1000");
        request.setEndUpdateTime("2000");

        assertThat(apiService.queryPddOrders2(request).isSuccess()).isTrue();
    }

    @Test
    void shouldSearchPddGoodsDetail() {
        ZhetaokeResponse<List<ZhetaokeItem>> mockResponse = createSuccessResponse(List.of());

        when(client.searchPddGoodsDetail(eq("test-appkey"), eq("pdd-key"), eq("pdd-secret"),
                eq("pid-1"), isNull(), eq("手机"), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        PddGoodsSearchRequest request = new PddGoodsSearchRequest();
        request.setPddAppKey("pdd-key");
        request.setPddAppSecret("pdd-secret");
        request.setPid("pid-1");
        request.setKeyword("手机");

        assertThat(apiService.searchPddGoodsDetail(request).isSuccess()).isTrue();
    }

    @Test
    void shouldGetPddGoodsCats() {
        ZhetaokeResponse<List<String>> mockResponse = createSuccessResponse(List.of("cat1", "cat2"));

        when(client.getPddGoodsCats(eq("test-appkey"), eq("pdd-key"), eq("pdd-secret"), eq("0")))
                .thenReturn(mockResponse);

        PddGoodsCatsRequest request = new PddGoodsCatsRequest();
        request.setPddAppKey("pdd-key");
        request.setPddAppSecret("pdd-secret");
        request.setParentCatId("0");

        ZhetaokeResponse<List<String>> result = apiService.getPddGoodsCats(request);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getContent()).hasSize(2);
    }

    private <T> ZhetaokeResponse<T> createSuccessResponse(T content) {
        ZhetaokeResponse<T> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent(content);
        return response;
    }
}
