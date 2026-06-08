package com.eagle.es.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ElasticSearchProperties}.
 *
 * <p>Verifies field defaults and that setter/getter round-trips work correctly.
 * No Spring context is loaded — all tests operate on plain POJO instances.
 */
@DisplayName("ElasticSearchProperties")
class ElasticSearchPropertiesTest {

    // -----------------------------------------------------------------------
    // Default values
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("default values")
    class DefaultValues {

        @Test
        @DisplayName("应Have默认Uris")
        void shouldHaveDefaultUris() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertNotNull(properties.getUris());
            assertEquals(1, properties.getUris().size());
            assertEquals("http://localhost:9200", properties.getUris().get(0));
        }

        @Test
        @DisplayName("应Havenull用户名通过默认")
        void shouldHaveNullUsernameByDefault() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertNull(properties.getUsername());
        }

        @Test
        @DisplayName("应Havenull密码通过默认")
        void shouldHaveNullPasswordByDefault() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertNull(properties.getPassword());
        }

        @Test
        @DisplayName("应Have默认连接Timeout")
        void shouldHaveDefaultConnectTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertEquals(5000, properties.getConnectTimeout());
        }

        @Test
        @DisplayName("应Have默认SocketTimeout")
        void shouldHaveDefaultSocketTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertEquals(30000, properties.getSocketTimeout());
        }

        @Test
        @DisplayName("应HaveSsl已禁用通过默认")
        void shouldHaveSslDisabledByDefault() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertFalse(properties.isSslEnabled());
        }
    }

    // -----------------------------------------------------------------------
    // Custom values via setters (@Data generates them)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("custom values")
    class CustomValues {

        @Test
        @DisplayName("应Accept自定义Uris")
        void shouldAcceptCustomUris() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            List<String> uris = List.of("http://es-node1:9200", "http://es-node2:9200");
            properties.setUris(uris);
            assertEquals(uris, properties.getUris());
        }

        @Test
        @DisplayName("应Accept自定义用户名")
        void shouldAcceptCustomUsername() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setUsername("eagle-user");
            assertEquals("eagle-user", properties.getUsername());
        }

        @Test
        @DisplayName("应Accept自定义密码")
        void shouldAcceptCustomPassword() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setPassword("s3cr3t");
            assertEquals("s3cr3t", properties.getPassword());
        }

        @Test
        @DisplayName("应Accept自定义连接Timeout")
        void shouldAcceptCustomConnectTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setConnectTimeout(3000);
            assertEquals(3000, properties.getConnectTimeout());
        }

        @Test
        @DisplayName("应Accept自定义SocketTimeout")
        void shouldAcceptCustomSocketTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setSocketTimeout(60000);
            assertEquals(60000, properties.getSocketTimeout());
        }

        @Test
        @DisplayName("应AcceptSslEnabled")
        void shouldAcceptSslEnabled() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setSslEnabled(true);
            assertTrue(properties.isSslEnabled());
        }
    }

    // -----------------------------------------------------------------------
    // equals / hashCode (Lombok @Data)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("two默认Instances应Be相等")
        void twoDefaultInstancesShouldBeEqual() {
            ElasticSearchProperties a = new ElasticSearchProperties();
            ElasticSearchProperties b = new ElasticSearchProperties();
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同Uris应不Be相等")
        void differentUrisShouldNotBeEqual() {
            ElasticSearchProperties a = new ElasticSearchProperties();
            ElasticSearchProperties b = new ElasticSearchProperties();
            b.setUris(List.of("http://remote-es:9200"));
            assertNotEquals(a, b);
        }
    }
}
