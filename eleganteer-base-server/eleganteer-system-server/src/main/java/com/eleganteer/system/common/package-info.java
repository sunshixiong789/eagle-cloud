/**
 * 共享内核（Shared Kernel）
 * <p>
 * 这是一个特殊的包，包含所有模块共享的通用组件。
 * <p>
 * 包含：
 * <ul>
 *   <li>dto/ - 通用的数据传输对象（如 ApiResponse、ErrorResult）</li>
 *   <li>exception/ - 通用异常类</li>
 *   <li>constant/ - 通用常量</li>
 *   <li>event/ - 通用事件定义</li>
 *   <li>i18n/ - 国际化支持</li>
 * </ul>
 * <p>
 * 注意：
 * <ul>
 *   <li>共享内核不是一个业务模块，不应该使用 @ApplicationModule 标注</li>
 *   <li>所有业务模块都可以依赖共享内核</li>
 *   <li>共享内核应该保持稳定，避免频繁变更</li>
 *   <li>不要在共享内核中放置业务逻辑</li>
 * </ul>
 *
 * @author 孙士雄
 * @since 1.0.0
 */
@NullMarked
package com.eleganteer.system.common;

import org.jspecify.annotations.NullMarked;
