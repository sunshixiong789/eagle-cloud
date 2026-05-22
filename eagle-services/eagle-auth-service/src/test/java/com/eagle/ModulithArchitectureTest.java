package com.eagle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * eagle-auth-service 单模块版 Modulith 边界验证。
 *
 * <p>auth 服务当前仅含 {@code com.eagle.auth} 一个有界上下文。{@link ApplicationModules#verify()}
 * 在单模块场景意义有限（且会把 classpath 上的 starter 包识别为 sibling module 触发误报），
 * 因此本测试只断言 auth 模块本身的命名接口契约，确保跨服务集成（Phase 3 切换为
 * RocketMQ JSON + 自建 RestClient）的依赖基线没被破坏。
 *
 * <p>未来若拆出 session / keystore 等子模块，再补充 {@code MODULES.verify()} 与跨模块约束。
 *
 * @author sunshixiong
 */
@DisplayName("Eagle Auth Modulith 模块边界验证")
class ModulithArchitectureTest {

    private static final ApplicationModules MODULES = ApplicationModules.of("com.eagle.auth");

    private static void assertNamedInterfacePresent(String moduleName, String interfaceName) {
        ApplicationModule module = MODULES.getModuleByName(moduleName)
                .orElseThrow(() -> new AssertionError(moduleName + " 模块未找到"));
        if (module.getNamedInterfaces().getByName(interfaceName).isEmpty()) {
            fail(moduleName + " 缺少 '" + interfaceName + "' 命名接口（@NamedInterface），"
                    + "请检查对应 package-info.java");
        }
    }

    @Test
    @DisplayName("auth 模块应存在")
    void authModuleShouldExist() {
        MODULES.getModuleByName("auth")
                .orElseThrow(() -> new AssertionError("auth 模块未找到（应在 com.eagle.auth 包下）"));
    }

    @Test
    @DisplayName("auth 应暴露 port / event / security / domain-services 四组命名接口")
    void shouldExposeAllNamedInterfaces() {
        assertNamedInterfacePresent("auth", "port");
        assertNamedInterfacePresent("auth", "event");
        assertNamedInterfacePresent("auth", "security");
        assertNamedInterfacePresent("auth", "domain-services");
    }
}
