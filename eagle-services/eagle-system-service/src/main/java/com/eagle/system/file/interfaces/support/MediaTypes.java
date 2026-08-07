package com.eagle.system.file.interfaces.support;

import org.springframework.http.MediaType;

/**
 * 下载响应的 MediaType 解析工具。
 *
 * @author sunshixiong
 */
public final class MediaTypes {

    private MediaTypes() {
    }

    /**
     * 把存储层记录的 contentType 解析为 {@link MediaType}，非法或缺失时降级为
     * {@code application/octet-stream}。
     *
     * <p>contentType 由上传方决定，历史数据可能为空或不合法。这里捕获的是
     * <b>格式解析异常</b>而非业务异常——不该交给全局异常处理器，否则一个坏
     * contentType 就会让下载接口返回 500，而正确行为是让浏览器按二进制下载。
     */
    public static MediaType parseOrOctetStream(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
