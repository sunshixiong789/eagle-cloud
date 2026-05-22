package com.eagle.auth.infrastructure.config;

import com.eagle.auth.infrastructure.security.JwtKeyProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 从 PKCS12 keystore 加载多 RSA key 组装为 {@link JWKSet}。
 *
 * <p>active key 在首位（用作签名），其余 previous key 同样加入 JWKSet 暴露给资源服务器，
 * 每个 key 以 alias 作为 {@code kid}。Nimbus 的 {@code JwtEncoder} 默认选第一个匹配
 * algorithm 的 key 用于签名，因此 active alias 必须保持在前。
 *
 * @author sunshixiong
 */
@Slf4j
@UtilityClass
class JwtKeySetLoader {

    static JWKSet loadAll(KeyStore keyStore, char[] password, JwtKeyProperties properties) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(properties.getKeyAlias());
        if (properties.getPreviousKeyAliases() != null) {
            aliases.addAll(properties.getPreviousKeyAliases());
        }

        List<RSAKey> keys = new ArrayList<>();
        for (String alias : aliases) {
            try {
                if (!keyStore.containsAlias(alias)) {
                    log.warn("JWT key alias not found in keystore, skip: {}", alias);
                    continue;
                }
                RSAKey raw = RSAKey.load(keyStore, alias, password);
                if (raw == null) {
                    log.warn("JWT alias is not RSA key, skip: {}", alias);
                    continue;
                }
                RSAKey withKid = new RSAKey.Builder(raw)
                        .keyID(alias)
                        .keyUse(KeyUse.SIGNATURE)
                        .build();
                keys.add(withKid);
            } catch (KeyStoreException | JOSEException ex) {
                log.error("failed to load JWT key alias: {}", alias, ex);
            }
        }
        if (keys.isEmpty()) {
            throw new IllegalStateException(
                    "no JWT signing keys loaded from keystore (active alias=" + properties.getKeyAlias() + ")");
        }
        return new JWKSet(List.copyOf(keys));
    }
}
