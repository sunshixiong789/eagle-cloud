package com.eagle.auth.core.infrastructure.external;

import com.eagle.auth.core.domain.AuthErrorCode;
import com.eagle.auth.core.infrastructure.config.AppleAuthenticationProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Apple /auth/token 与 /auth/revoke 的固定域名客户端。 */
@Slf4j
@Component
public class AppleTokenClient {

    private final AppleAuthenticationProperties properties;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final RestClient restClient;

    public AppleTokenClient(
            AppleAuthenticationProperties properties,
            AppleClientSecretGenerator clientSecretGenerator,
            @Qualifier("appleRestClient") RestClient restClient) {
        this.properties = properties;
        this.clientSecretGenerator = clientSecretGenerator;
        this.restClient = restClient;
    }

    public AppleTokenSet exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        try {
            AppleTokenResponse response = restClient.post()
                    .uri(properties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);
            if (response == null || isBlank(response.idToken())
                    || isBlank(response.refreshToken())) {
                throw AuthErrorCode.APPLE_TOKEN_EXCHANGE_FAILED.toDomainException();
            }
            return new AppleTokenSet(response.idToken(), response.refreshToken());
        } catch (RestClientException ex) {
            log.warn("Apple authorization-code exchange failed, status={}", statusCode(ex));
            throw AuthErrorCode.APPLE_TOKEN_EXCHANGE_FAILED.toDomainException();
        }
    }

    public void revoke(String refreshToken) {
        MultiValueMap<String, String> form = baseForm();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            restClient.post()
                    .uri(properties.getRevokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 400
                    && ex.getResponseBodyAsString().contains("invalid_grant")) {
                log.info("Apple refresh token was already invalidated");
                return;
            }
            log.warn("Apple token revocation failed, status={}", ex.getStatusCode().value());
            throw AuthErrorCode.APPLE_TOKEN_REVOCATION_FAILED.toDomainException();
        } catch (RestClientException ex) {
            log.warn("Apple token revocation failed due to transport error");
            throw AuthErrorCode.APPLE_TOKEN_REVOCATION_FAILED.toDomainException();
        }
    }

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", clientSecretGenerator.generate());
        return form;
    }

    private Integer statusCode(RestClientException ex) {
        return ex instanceof RestClientResponseException response
                ? response.getStatusCode().value() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record AppleTokenResponse(
            @JsonProperty("id_token") String idToken,
            @JsonProperty("refresh_token") String refreshToken) {
    }

    public record AppleTokenSet(String identityToken, String refreshToken) {
    }
}
