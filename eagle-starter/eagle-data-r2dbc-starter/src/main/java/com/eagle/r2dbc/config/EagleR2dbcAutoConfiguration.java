package com.eagle.r2dbc.config;

import com.eagle.r2dbc.properties.R2dbcProperties;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

/**
 * Eagle R2DBC auto-configuration entry.
 *
 * <p>Spring Boot creates the {@link ConnectionFactory} and {@link R2dbcEntityTemplate}
 * from {@code spring.r2dbc.*}. This auto-configuration keeps Eagle-specific
 * properties discoverable and provides a single switch for reactive data access.
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@ConditionalOnClass({ConnectionFactory.class, R2dbcEntityTemplate.class})
@ConditionalOnProperty(name = "eagle.r2dbc.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(R2dbcProperties.class)
public class EagleR2dbcAutoConfiguration {
}
