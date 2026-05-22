package com.eagle.r2dbc.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;

/**
 * Eagle R2DBC auto-configuration entry.
 *
 * <p>Spring Boot creates the {@link ConnectionFactory} and {@link R2dbcEntityTemplate}
 * from {@code spring.r2dbc.*}. Eagle R2DBC support activates whenever the reactive
 * data classes are on the classpath.
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@ConditionalOnClass({ConnectionFactory.class, R2dbcEntityTemplate.class})
public class EagleR2dbcAutoConfiguration {
}
