package com.eagle.datapermission.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据权限配置属性。
 *
 * @author eagle
 */
@Data
@ConfigurationProperties(prefix = "eagle.data-permission")
public class DataPermissionProperties {

    /**
     * 默认部门字段名。
     */
    private String defaultDeptField = "deptId";

    /**
     * 默认用户字段名。
     */
    private String defaultUserField = "id";
}
