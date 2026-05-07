package com.eagle.zhetaoke.meituan;

import com.eagle.zhetaoke.dto.ZhetaokeLinkResult;
import com.eagle.zhetaoke.dto.ZhetaokeOrder;
import com.eagle.zhetaoke.dto.ZhetaokeResponse;
import com.eagle.zhetaoke.meituan.client.MeituanApiService;
import com.eagle.zhetaoke.meituan.client.MeituanClient;
import com.eagle.zhetaoke.meituan.request.MeituanLinkConvertRequest;
import com.eagle.zhetaoke.meituan.request.MeituanOrderQueryRequest;
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
 * {@link MeituanApiService} 单元测试。
 *
 * @author 孙士雄
 */
class MeituanApiServiceTest {

    @Mock
    private MeituanClient client;

    @Mock
    private ZhetaokeProperties properties;

    @InjectMocks
    private MeituanApiService apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(properties.getAppkey()).thenReturn("test-appkey");
    }

    @Test
    void shouldGenerateMeituanLinkWithSimpleParams() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.generateMeituanLink(eq("test-appkey"), eq("sid-1"), eq("7"), eq("1"),
                isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.generateMeituanLink("sid-1", "7", "1").isSuccess()).isTrue();
    }

    @Test
    void shouldGenerateMeituanLinkWithRequest() {
        ZhetaokeResponse<ZhetaokeLinkResult> mockResponse = new ZhetaokeResponse<>();
        mockResponse.setStatus(200);

        when(client.generateMeituanLink(eq("test-appkey"), eq("sid-1"), eq("7"), eq("1"),
                eq("1"), isNull(), isNull(), isNull(), eq("123")))
                .thenReturn(mockResponse);

        MeituanLinkConvertRequest request = new MeituanLinkConvertRequest();
        request.setSid("sid-1");
        request.setActId("7");
        request.setLinkType("1");
        request.setMiniCode("1");
        request.setCustomerId("123");

        assertThat(apiService.generateMeituanLink(request).isSuccess()).isTrue();
    }

    @Test
    void shouldQueryMeituanOrders2WithSimpleParams() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());

        when(client.queryMeituanOrders2(eq("test-appkey"), eq("1"), eq("1"), eq("50"),
                isNull(), isNull(), isNull(), isNull()))
                .thenReturn(mockResponse);

        assertThat(apiService.queryMeituanOrders2("1", "1", "50").isSuccess()).isTrue();
    }

    @Test
    void shouldQueryMeituanOrders2WithRequest() {
        ZhetaokeResponse<List<ZhetaokeOrder>> mockResponse = createSuccessResponse(List.of());

        when(client.queryMeituanOrders2(eq("test-appkey"), eq("1"), eq("1"), eq("50"),
                eq("2024-01-01"), eq("2024-01-31"), isNull(), isNull()))
                .thenReturn(mockResponse);

        MeituanOrderQueryRequest request = new MeituanOrderQueryRequest();
        request.setType("1");
        request.setPage("1");
        request.setPageSize("50");
        request.setStartTime("2024-01-01");
        request.setEndTime("2024-01-31");

        assertThat(apiService.queryMeituanOrders2(request).isSuccess()).isTrue();
    }

    private <T> ZhetaokeResponse<T> createSuccessResponse(T content) {
        ZhetaokeResponse<T> response = new ZhetaokeResponse<>();
        response.setStatus(200);
        response.setContent(content);
        return response;
    }
}
