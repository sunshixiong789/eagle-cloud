package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.domain.service.TaobaoService;
import com.taobao.api.ApiException;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.OpenuidGetRequest;
import com.taobao.api.request.TopAuthTokenCreateRequest;
import com.taobao.api.response.OpenuidGetResponse;
import com.taobao.api.response.TopAuthTokenCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link TaobaoService} 的 TOP SDK 实现。
 *
 * <p>主路径（百川 native 一键授权）：access token 作为 TOP session 调 {@code taobao.openuid.get}
 * 直取 {@code open_uid}。兜底路径（授权码）：调 {@code taobao.top.auth.token.create} 用授权码换
 * {@code token_result}（JSON），从中取 {@code taobao_open_uid}。两者都得到稳定身份标识。
 *
 * <p>{@code eagle.taobao.app.enabled=false} 时 {@link TaobaoClient} 未装配，
 * 用 {@link ObjectProvider} 延迟解析，缺失即抛 {@code TAOBAO_UPSTREAM}。
 *
 * <p><strong>SDK API 核对说明</strong>：实际 jar（{@code taobao-sdk-java-auto}，与
 * ease-mind-servers/zhetaoke-starter 同款本地 jar）中 {@code isSuccess()} /
 * {@code getErrorCode()} / {@code getSubCode()} / {@code getSubMsg()} 定义在
 * 父类 {@code TaobaoResponse} 上（非 {@code isError()}），与
 * {@code TopPublisherInfoSaveTaobaoSdkAdapter} 的用法一致，已对照 javap 输出确认。
 *
 * @author sunshixiong
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaobaoServiceImpl implements TaobaoService {

    private final ObjectProvider<TaobaoClient> taobaoClientProvider;
    private final ObjectMapper objectMapper;

    @Override
    public String resolveOpenUid(String tbAccessToken, String tbAuthCode) {
        boolean hasToken = tbAccessToken != null && !tbAccessToken.isBlank();
        boolean hasCode = tbAuthCode != null && !tbAuthCode.isBlank();
        if (!hasToken && !hasCode) {
            throw AuthErrorCode.TAOBAO_AUTH_REQUIRED.toDomainException();
        }
        TaobaoClient client = taobaoClientProvider.getIfAvailable();
        if (client == null) {
            log.warn("TaobaoClient 未装配（eagle.taobao.app.enabled=false?）");
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
        }
        // 百川 native SDK 授权返回的是 access token（非授权码），优先用它凭 session 直取 openUid；
        // tb_auth_code 路径保留作兜底（将来 Web/H5 授权码流可复用）。
        return hasToken
                ? resolveByAccessToken(client, tbAccessToken)
                : resolveByAuthCode(client, tbAuthCode);
    }

    /**
     * 主路径：access token 作为 TOP session 调 {@code taobao.openuid.get} 直取 openUid。
     */
    private String resolveByAccessToken(TaobaoClient client, String accessToken) {
        OpenuidGetRequest req = new OpenuidGetRequest();
        OpenuidGetResponse resp;
        try {
            resp = client.execute(req, accessToken);
        } catch (ApiException ex) {
            log.warn("TOP openuid.get 调用异常", ex);
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException(ex);
        }
        if (resp == null || !resp.isSuccess()
                || resp.getOpenUid() == null || resp.getOpenUid().isBlank()) {
            log.warn("TOP openuid.get 失败: code={}, subCode={}, subMsg={}",
                    resp != null ? resp.getErrorCode() : null,
                    resp != null ? resp.getSubCode() : null,
                    resp != null ? resp.getSubMsg() : "null response");
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
        }
        return resp.getOpenUid();
    }

    /**
     * 兜底路径：用 TOP 授权码调 {@code taobao.top.auth.token.create} 换 token_result，
     * 从中取 taobao_open_uid。
     */
    private String resolveByAuthCode(TaobaoClient client, String topAuthCode) {
        TopAuthTokenCreateRequest req = new TopAuthTokenCreateRequest();
        req.setCode(topAuthCode);

        TopAuthTokenCreateResponse resp;
        try {
            resp = client.execute(req);
        } catch (ApiException ex) {
            log.warn("TOP top.auth.token.create 调用异常", ex);
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException(ex);
        }
        if (resp == null || !resp.isSuccess() || resp.getTokenResult() == null) {
            log.warn("TOP top.auth.token.create 失败: code={}, subCode={}, subMsg={}",
                    resp != null ? resp.getErrorCode() : null,
                    resp != null ? resp.getSubCode() : null,
                    resp != null ? resp.getSubMsg() : "null response");
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
        }
        return parseOpenUid(resp.getTokenResult());
    }

    private String parseOpenUid(String tokenResult) {
        try {
            JsonNode node = objectMapper.readTree(tokenResult);
            String openUid = node.path("taobao_open_uid").asString(null);
            if (openUid == null || openUid.isBlank()) {
                // 部分应用资质无 open_uid，退回 taobao_user_id 作为标识
                openUid = node.path("taobao_user_id").asString(null);
            }
            if (openUid == null || openUid.isBlank()) {
                // token_result 含明文 access_token / refresh_token，禁止整体落日志（13-logging.md）；
                // 仅输出字段名用于排查。
                log.warn("token_result 未含 taobao_open_uid/taobao_user_id, fields={}",
                        fieldNames(node));
                throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException();
            }
            return openUid;
        } catch (JacksonException ex) {
            // 不输出原始 tokenResult（可能含明文 token）；仅记录长度与异常。
            log.warn("解析 token_result 失败, length={}", tokenResult == null ? 0 : tokenResult.length(), ex);
            throw AuthErrorCode.TAOBAO_UPSTREAM.toServiceException(ex);
        }
    }

    /**
     * 提取 JSON 字段名列表（不含值），用于安全排查——避免把含明文 token 的值写进日志。
     */
    private static String fieldNames(JsonNode node) {
        return String.join(",", node.propertyNames());
    }
}
