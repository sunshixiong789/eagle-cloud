/**
 * 文件管理模块（File Bounded Context）
 * <p>
 * 职责：业务文件的元数据管理与对象存储集成（上传、下载、软删除）。
 * <p>
 * 通过 {@code eagle-oss-minio-starter} 提供的 {@link com.eagle.oss.service.StorageService}
 * 接口与底层存储解耦，支持 local / minio / oss 多种实现，由配置 {@code eagle.storage.type} 选择。
 * <p>
 * <strong>依赖约束</strong>
 * <ul>
 *   <li>仅依赖 {@code common}（共享内核：异常体系、基础 DTO、聚合根基类）</li>
 *   <li>不依赖 auth / base 模块：文件归属仅按 uploaded_by（user id）追踪，跨域查询走 Port</li>
 * </ul>
 *
 * @author sunshixiong
 */
@ApplicationModule(
        displayName = "文件管理模块"
)
@NullMarked
package com.eagle.system.file;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
