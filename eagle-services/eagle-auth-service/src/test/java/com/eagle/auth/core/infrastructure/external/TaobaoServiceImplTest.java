package com.eagle.auth.core.infrastructure.external;

import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.ServiceException;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.OpenuidGetRequest;
import com.taobao.api.response.OpenuidGetResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaobaoServiceImplTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<TaobaoClient> clientProvider = mock(ObjectProvider.class);
    private final TaobaoClient client = mock(TaobaoClient.class);
    private final TaobaoServiceImpl service =
            new TaobaoServiceImpl(clientProvider, mock(ObjectMapper.class));

    @Test
    @DisplayName("有 access token → 用 taobao.openuid.get 凭 session 取 openUid（百川一键授权主路径）")
    void resolvesOpenUidByAccessToken() throws Exception {
        when(clientProvider.getIfAvailable()).thenReturn(client);
        OpenuidGetResponse resp = new OpenuidGetResponse();
        resp.setOpenUid("open-uid-123");
        when(client.execute(any(OpenuidGetRequest.class), eq("acc-token"))).thenReturn(resp);

        String openUid = service.resolveOpenUid("acc-token", null);

        assertEquals("open-uid-123", openUid);
    }

    @Test
    @DisplayName("access token 与 auth code 均为空 → 抛 DomainException（淘宝授权信息不能为空）")
    void rejectsWhenBothBlank() {
        assertThrows(DomainException.class, () -> service.resolveOpenUid(null, "  "));
    }

    @Test
    @DisplayName("openuid.get 上游失败（subCode 非空）→ 抛 ServiceException")
    void wrapsUpstreamFailure() throws Exception {
        when(clientProvider.getIfAvailable()).thenReturn(client);
        OpenuidGetResponse resp = new OpenuidGetResponse();
        resp.setSubCode("isv.invalid-session");
        when(client.execute(any(OpenuidGetRequest.class), eq("bad-token"))).thenReturn(resp);

        assertThrows(ServiceException.class, () -> service.resolveOpenUid("bad-token", null));
    }
}
