package com.eagle.auth.infrastructure.external.provider;

import com.eagle.auth.infrastructure.config.PhoneOneClickProperties;
import com.eagle.common.exception.AppException;
import com.tencentcloudapi.common.CommonClient;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TencentPhoneOneClickProviderTest {

    @Mock
    private CommonClient commonClient;

    private PhoneOneClickProperties properties;
    private TencentPhoneOneClickProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        properties = new PhoneOneClickProperties();
        properties.setProvider("tencent");
        provider = new TencentPhoneOneClickProvider(properties);
        injectClient(commonClient);
    }

    private void injectClient(CommonClient client) throws Exception {
        Field field = TencentPhoneOneClickProvider.class.getDeclaredField("tencentClient");
        field.setAccessible(true);
        field.set(provider, client);
    }

    @Test
    @DisplayName("name 返回 tencent")
    void shouldExposeTencentName() {
        assertEquals("tencent", provider.name());
    }

    @Nested
    @DisplayName("verifyAndGetPhone")
    class VerifyAndGetPhone {

        @Test
        @DisplayName("成功响应解析出手机号（含 Response wrapper）")
        void shouldParsePhoneFromResponseWrapper() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"Mobile\":\"13812345678\",\"Code\":\"Ok\",\"RequestId\":\"req-1\"}}");
            assertEquals("13812345678", provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("成功响应解析出手机号（无 Response wrapper）")
        void shouldParsePhoneFromBareBody() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Mobile\":\"13812345678\",\"Code\":\"Ok\"}");
            assertEquals("13812345678", provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("自动剥离 +86 前缀")
        void shouldStripCountryCodePrefix() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"Mobile\":\"+8613812345678\",\"Code\":\"Ok\"}}");
            assertEquals("13812345678", provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("响应携带 Error 字段时拒绝")
        void shouldRejectWhenErrorReturned() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"Error\":{\"Code\":\"InvalidParameter\",\"Message\":\"bad token\"},\"RequestId\":\"req-2\"}}");
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("业务 Code 与 successCode 不匹配时拒绝")
        void shouldRejectWhenCodeMismatch() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"Mobile\":\"13812345678\",\"Code\":\"Fail\"}}");
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("成功响应无 Code 字段也接受（部分产品不返回 Code）")
        void shouldAcceptWhenCodeAbsent() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"Mobile\":\"13812345678\"}}");
            assertEquals("13812345678", provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("手机号字段为空时拒绝")
        void shouldRejectWhenPhoneFieldBlank() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"Mobile\":\"\",\"Code\":\"Ok\"}}");
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("手机号格式非法时拒绝")
        void shouldRejectWhenPhoneFormatInvalid() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"Mobile\":\"12345\",\"Code\":\"Ok\"}}");
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("SDK 抛异常时转为 ServiceException")
        void shouldWrapSdkException() throws Exception {
            when(commonClient.call(anyString(), anyString()))
                    .thenThrow(new TencentCloudSDKException("network failure"));
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("响应为空字符串时拒绝")
        void shouldRejectWhenResponseEmpty() throws Exception {
            when(commonClient.call(anyString(), anyString())).thenReturn("");
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("自定义 phoneField 时按配置取字段")
        void shouldUseConfiguredPhoneField() throws Exception {
            properties.getTencent().setPhoneField("PhoneNumber");
            when(commonClient.call(anyString(), anyString()))
                    .thenReturn("{\"Response\":{\"PhoneNumber\":\"13812345678\",\"Code\":\"Ok\"}}");
            assertEquals("13812345678", provider.verifyAndGetPhone("token"));
        }

        @Test
        @DisplayName("tencentClient 未初始化时拒绝")
        void shouldRejectWhenClientNotInitialized() throws Exception {
            injectClient(null);
            assertThrows(AppException.class, () -> provider.verifyAndGetPhone("token"));
        }
    }
}
