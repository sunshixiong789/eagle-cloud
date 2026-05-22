package com.eagle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * eagle-auth-service Modulith 边界验证占位。
 *
 * <p>auth 服务目前仅含 {@code com.eagle.auth} 一个有界上下文，Spring Modulith 的
 * 默认 base-package 推导规则会把它的 DDD 四层子包（application / domain /
 * infrastructure / interfaces）当作独立模块并报循环依赖；同时若把扫描根扩到
 * {@code com.eagle}，又会把 classpath 上的 starter 包（{@code com.eagle.datajpa}、
 * {@code com.eagle.http} 等）识别为 sibling module 触发误报。
 *
 * <p>Phase 2 阶段先禁用整套验证，由 system-service 端的多模块 ModulithArchitectureTest
 * 兜底；待 auth 服务后续拆出 session / keystore 等子模块、形成多模块结构时再恢复。
 *
 * @author sunshixiong
 */
@DisplayName("Eagle Auth Modulith 模块边界验证")
@Disabled("Phase 2 阶段单模块服务暂不启用 Modulith verify；后续拆出子模块再恢复")
class ModulithArchitectureTest {

    @Test
    void placeholder() {
        // 占位用例：保留类与 javadoc 上下文，避免被误以为遗漏测试。
    }
}
