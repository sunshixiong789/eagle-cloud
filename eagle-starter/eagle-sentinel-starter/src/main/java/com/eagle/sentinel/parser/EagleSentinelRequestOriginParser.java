package com.eagle.sentinel.parser;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Eagle Sentinel 请求来源解析器。
 *
 * <p>从请求头 {@code X-Application-Name} 中提取调用方应用名称，
 * 用于 Sentinel 授权规则（Authority Rule）中的来源白/黑名单控制。
 * 若请求头不存在，则返回 {@code "default"} 作为默认来源。
 *
 * <p>示例：在 Sentinel Dashboard 配置授权规则时，将 {@code X-Application-Name}
 * 设置为 "eagle-gateway" 的请求加入白名单，其他来源直接拒绝。
 *
 * @author 孙士雄
 */
public class EagleSentinelRequestOriginParser implements RequestOriginParser {

    /**
     * 用于传递调用方应用名的请求头名称。
     */
    private static final String HEADER_APP_NAME = "X-Application-Name";

    /**
     * 请求头不存在时的默认来源标识。
     */
    private static final String DEFAULT_ORIGIN = "default";

    /**
     * 从请求头 {@code X-Application-Name} 解析调用方来源。
     *
     * @param request HTTP 请求
     * @return 调用方应用名，不存在则返回 {@code "default"}
     */
    @Override
    public String parseOrigin(HttpServletRequest request) {
        String appName = request.getHeader(HEADER_APP_NAME);
        if (appName == null || appName.isBlank()) {
            return DEFAULT_ORIGIN;
        }
        return appName;
    }
}
