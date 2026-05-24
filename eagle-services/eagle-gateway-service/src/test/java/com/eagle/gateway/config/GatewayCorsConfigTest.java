package com.eagle.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GatewayCorsConfig fail-fast 校验")
class GatewayCorsConfigTest {

    private static GatewayCorsConfig build(String profile, List<String> patterns, boolean allowCredentials) {
        GatewayCorsProperties props = new GatewayCorsProperties();
        props.setAllowedOriginPatterns(patterns);
        props.setAllowCredentials(allowCredentials);
        MockEnvironment env = new MockEnvironment();
        if (profile != null) {
            env.setActiveProfiles(profile);
        }
        return new GatewayCorsConfig(props, env);
    }

    @Test
    @DisplayName("prod 下 allowedOriginPatterns=['*'] + allowCredentials=true → 抛异常")
    void rejectsWildcardWithCredentialsInProd() {
        GatewayCorsConfig config = build("prod", List.of("*"), true);

        assertThatThrownBy(config::assertSecureCorsInProd)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("allowCredentials=true")
                .hasMessageContaining("'*'");
    }

    @Test
    @DisplayName("staging 同样收紧")
    void rejectsWildcardWithCredentialsInStaging() {
        GatewayCorsConfig config = build("staging", List.of("*"), true);

        assertThatThrownBy(config::assertSecureCorsInProd)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("dev 下允许通配符 + credentials (本地联调)")
    void allowsWildcardWithCredentialsInDev() {
        GatewayCorsConfig config = build("dev", List.of("*"), true);

        assertThatCode(config::assertSecureCorsInProd).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("local 下允许通配符 + credentials")
    void allowsWildcardWithCredentialsInLocal() {
        GatewayCorsConfig config = build("local", List.of("*"), true);

        assertThatCode(config::assertSecureCorsInProd).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("prod 下显式域名 + credentials → 放行")
    void allowsExplicitOriginsInProd() {
        GatewayCorsConfig config = build("prod",
                List.of("https://eagle.com", "https://admin.eagle.com"), true);

        assertThatCode(config::assertSecureCorsInProd).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("prod 下 allowCredentials=false 即使 '*' 也放行 (CORS 规范允许)")
    void allowsWildcardWithoutCredentialsInProd() {
        GatewayCorsConfig config = build("prod", List.of("*"), false);

        assertThatCode(config::assertSecureCorsInProd).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("'*' 周围有空白也应识别 (防误配)")
    void rejectsWildcardWithWhitespace() {
        GatewayCorsConfig config = build("prod", List.of(" * "), true);

        assertThatThrownBy(config::assertSecureCorsInProd)
                .isInstanceOf(IllegalStateException.class);
    }
}
