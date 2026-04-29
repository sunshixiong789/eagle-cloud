/**
 * 共享内核（Shared Kernel）
 * <p>
 * 包含所有模块共享的通用组件，通过 {@link org.springframework.modulith.ApplicationModule.Type#OPEN}
 * 声明为完全开放模块：所有子包中的公开类型对所有模块可见，无需逐一声明。
 * <p>
 * <strong>包含</strong>
 * <ul>
 *   <li>{@code base/}        — JPA 基类（BaseAggregateRoot、BaseEntity）</li>
 *   <li>{@code dto/}         — 通用 DTO（ErrorResult）</li>
 *   <li>{@code exception/}   — 异常基础设施（AppException 层次、ErrorCode 接口）</li>
 *   <li>{@code exception/codes/} — 各域错误码枚举（CommonErrorCode、UserErrorCode 等）</li>
 *   <li>{@code constant/}    — 系统常量</li>
 *   <li>{@code i18n/}        — 国际化工具（MessageSourceUtil）</li>
 * </ul>
 * <p>
 * <strong>约定</strong>
 * <ul>
 *   <li>{@code common} 本身不得依赖任何业务模块（auth、system、config）</li>
 *   <li>只放跨模块公用的技术组件，不放业务逻辑</li>
 *   <li>保持高稳定性，变更需谨慎，影响所有模块</li>
 * </ul>
 *
 * @author sunshixiong
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
@NullMarked
package com.eagle.system.common;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
