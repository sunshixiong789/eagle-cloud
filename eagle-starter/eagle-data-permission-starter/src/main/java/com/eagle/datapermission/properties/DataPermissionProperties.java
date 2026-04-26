package com.eagle.datapermission.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 数据权限配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.data-permission")
public class DataPermissionProperties {

    /**
     * 是否启用数据权限。
     */
    private boolean enabled = true;

    /**
     * 默认部门字段名。
     */
    private String defaultDeptField = "deptId";

    /**
     * 默认用户字段名。
     */
    private String defaultUserField = "id";
}
