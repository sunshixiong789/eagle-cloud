package com.eagle.r2dbc.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Eagle R2DBC configuration properties.
 */
@Data
@ConfigurationProperties(prefix = "eagle.r2dbc")
public class R2dbcProperties {

    /**
     * Whether Eagle R2DBC support is enabled.
     */
    private boolean enabled = true;
}
