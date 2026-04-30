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
        @DisplayName("should default uris to [http://localhost:9200]")
        void shouldHaveDefaultUris() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertNotNull(properties.getUris());
            assertEquals(1, properties.getUris().size());
            assertEquals("http://localhost:9200", properties.getUris().get(0));
        }

        @Test
        @DisplayName("should default username to null")
        void shouldHaveNullUsernameByDefault() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertNull(properties.getUsername());
        }

        @Test
        @DisplayName("should default password to null")
        void shouldHaveNullPasswordByDefault() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertNull(properties.getPassword());
        }

        @Test
        @DisplayName("should default connectTimeout to 5000 ms")
        void shouldHaveDefaultConnectTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertEquals(5000, properties.getConnectTimeout());
        }

        @Test
        @DisplayName("should default socketTimeout to 30000 ms")
        void shouldHaveDefaultSocketTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            assertEquals(30000, properties.getSocketTimeout());
        }

        @Test
        @DisplayName("should default sslEnabled to false")
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
        @DisplayName("should accept custom uris")
        void shouldAcceptCustomUris() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            List<String> uris = List.of("http://es-node1:9200", "http://es-node2:9200");
            properties.setUris(uris);
            assertEquals(uris, properties.getUris());
        }

        @Test
        @DisplayName("should accept custom username")
        void shouldAcceptCustomUsername() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setUsername("eagle-user");
            assertEquals("eagle-user", properties.getUsername());
        }

        @Test
        @DisplayName("should accept custom password")
        void shouldAcceptCustomPassword() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setPassword("s3cr3t");
            assertEquals("s3cr3t", properties.getPassword());
        }

        @Test
        @DisplayName("should accept custom connectTimeout")
        void shouldAcceptCustomConnectTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setConnectTimeout(3000);
            assertEquals(3000, properties.getConnectTimeout());
        }

        @Test
        @DisplayName("should accept custom socketTimeout")
        void shouldAcceptCustomSocketTimeout() {
            ElasticSearchProperties properties = new ElasticSearchProperties();
            properties.setSocketTimeout(60000);
            assertEquals(60000, properties.getSocketTimeout());
        }

        @Test
        @DisplayName("should accept sslEnabled = true")
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
        @DisplayName("two default instances should be equal")
        void twoDefaultInstancesShouldBeEqual() {
            ElasticSearchProperties a = new ElasticSearchProperties();
            ElasticSearchProperties b = new ElasticSearchProperties();
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("instances with different uris should not be equal")
        void differentUrisShouldNotBeEqual() {
            ElasticSearchProperties a = new ElasticSearchProperties();
            ElasticSearchProperties b = new ElasticSearchProperties();
            b.setUris(List.of("http://remote-es:9200"));
            assertNotEquals(a, b);
        }
    }
}
