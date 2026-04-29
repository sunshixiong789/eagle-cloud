package com.eagle.feign.properties;

import feign.Logger;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Eagle Feign 配置属性。
 *
 * @author 孙士雄
 */
@Data
@ConfigurationProperties(prefix = "eagle.feign")
public class FeignProperties {

    /**
     * Feign 客户端日志级别。
     * <ul>
     *   <li>NONE — 无日志（生产推荐）</li>
     *   <li>BASIC — 记录请求方法、URL 和响应状态码（默认）</li>
     *   <li>HEADERS — BASIC + 请求/响应头</li>
     *   <li>FULL — 全量日志，含请求体（仅调试使用）</li>
     * </ul>
     */
    private Logger.Level logLevel = Logger.Level.BASIC;

    /**
     * 连接超时，单位毫秒，默认 2 秒。
     */
    private int connectTimeout = 2000;

    /**
     * 读取超时，单位毫秒，默认 5 秒。
     */
    private int readTimeout = 5000;
}
