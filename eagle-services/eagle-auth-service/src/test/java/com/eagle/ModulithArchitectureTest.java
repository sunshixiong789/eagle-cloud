package com.eagle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * eagle-auth-service 单模块版 Modulith 边界验证。
 *
 * <p>auth 服务当前仅含 {@code com.eagle.auth} 一个有界上下文，未来若拆出
 * session/keystore 等子模块需在此扩展断言。
 *
 * <p>命名接口仍按搬迁前保留（{@code port}/{@code event}/{@code security}/
 * {@code domain-services}）——拆服务后跨服务通过 RocketMQ + 自建 RestClient
 * 通信，名义接口仅作为同服务内的依赖约束基线。
 *
 * @author sunshixiong
 */
@DisplayName("Eagle Auth Modulith 模块边界验证")
class ModulithArchitectureTest {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(EagleAuthApplication.class);

    private static void assertNamedInterfacePresent(String moduleName, String interfaceName) {
        ApplicationModule module = MODULES.getModuleByName(moduleName)
                .orElseThrow(() -> new AssertionError(moduleName + " 模块未找到"));
        if (module.getNamedInterfaces().getByName(interfaceName).isEmpty()) {
            fail(moduleName + " 缺少 '" + interfaceName + "' 命名接口（@NamedInterface），"
                    + "请检查对应 package-info.java");
        }
    }

    @Test
    @DisplayName("verify() 验证模块依赖合法、无循环依赖、无非法跨模块访问")
    void verifiesModularStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("生成模块依赖图文档（PlantUML）")
    void writeDocumentationSnippets() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
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
