package com.eagle.oss.service;

import java.io.InputStream;

/**
 * 文件存储服务接口。
 *
 * <p>统一抽象上传、下载、删除、获取访问 URL 能力。
 *
 * @author 孙士雄
 */
public interface StorageService {

    /**
     * 上传文件。
     *
     * @param bucket   存储桶/目录
     * @param path     文件路径
     * @param input    文件输入流
     * @param size     文件大小
     * @param mimeType MIME 类型
     * @return 文件访问 URL
     */
    String upload(String bucket, String path, InputStream input, long size, String mimeType);

    /**
     * 下载文件。
     *
     * @param bucket 存储桶/目录
     * @param path   文件路径
     * @return 文件输入流
     */
    InputStream download(String bucket, String path);

    /**
     * 删除文件。
     *
     * @param bucket 存储桶/目录
     * @param path   文件路径
     */
    void delete(String bucket, String path);

    /**
     * 获取文件访问 URL。
     *
     * @param bucket 存储桶/目录
     * @param path   文件路径
     * @return 访问 URL
     */
    String getUrl(String bucket, String path);
}
