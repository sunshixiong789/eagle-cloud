package com.eagle.xxljob.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * XXL-Job 执行器配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.xxl-job")
public class XxlJobProperties {

    /**
     * 是否启用 XXL-Job 执行器。
     */
    private boolean enabled = true;

    /**
     * 调度中心地址，多个用逗号分隔。
     */
    private String adminAddresses = "http://localhost:8080/xxl-job-admin";

    /**
     * 访问令牌。
     */
    private String accessToken = "";

    /**
     * 执行器 AppName。
     */
    private String appName = "";

    /**
     * 执行器 IP（为空则自动获取）。
     */
    private String ip = "";

    /**
     * 执行器端口（为空则自动获取可用端口）。
     */
    private int port = 0;

    /**
     * 执行器日志路径。
     */
    private String logPath = "/data/applogs/xxl-job/jobhandler";

    /**
     * 执行器日志保留天数。
     */
    private int logRetentionDays = 30;
}
