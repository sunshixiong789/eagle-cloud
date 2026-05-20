package com.eagle.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Spring Modulith 模块边界验证测试
 * <p>
 * 模块划分（有界上下文）：
 * <pre>
 *   com.eagle.system.auth   — 认证授权域，定义 AuthorizationPort（六边形架构 Driven Port）
 *   com.eagle.system.base   — 系统管理域，依赖 auth::port + auth::event
 *   com.eagle.system.config — 全局配置（粘合层，引用 auth::security 装配安全过滤链）
 *   com.eagle.system.common — 共享内核（异常体系、基础 DTO）
 * </pre>
 * <p>
 * <strong>强制约束（{@link ApplicationModules#verify()} 自动检测）</strong>
 * <ul>
 *   <li>auth 不依赖 base / config（{@code allowedDependencies = {}}）</li>
 *   <li>base 仅依赖 {@code auth::port} 与 {@code auth::event}（{@code @ApplicationModule} 声明）</li>
 *   <li>跨域事件定义在发布方的 {@code auth/domain/event/}，通过 {@code @NamedInterface("event")} 暴露</li>
 *   <li>所有模块间无循环依赖</li>
 * </ul>
 *
 * <p><strong>运行</strong>
 * <pre>gradle test --tests "com.eagle.system.ModulithArchitectureTest"</pre>
 *
 * @author sunshixiong
 */
@DisplayName("Spring Modulith 模块边界验证")
class ModulithArchitectureTest {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(EagleSystemApplication.class);

    private static void assertNamedInterfacePresent(String moduleName, String interfaceName) {
        ApplicationModule module = MODULES.getModuleByName(moduleName)
                .orElseThrow(() -> new AssertionError(moduleName + " 模块未找到"));
        if (module.getNamedInterfaces().getByName(interfaceName).isEmpty()) {
            fail(moduleName + " 缺少 '" + interfaceName + "' 命名接口（@NamedInterface），"
                    + "请检查对应 package-info.java");
        }
    }

    /**
     * 断言模块的（直接或传递）依赖中不包含目标模块。
     */
    private static void assertModuleNotDependsOn(ApplicationModule module, String forbiddenModule) {
        boolean depends = module.getDependencies(MODULES).stream()
                .map(dep -> dep.getTargetModule().getName())
                .anyMatch(forbiddenModule::equals);
        if (depends) {
            fail(module.getName() + " 不应依赖 " + forbiddenModule + " 模块；"
                    + "请检查 import 语句和 package-info.java 的 allowedDependencies");
        }
    }

    @Test
    @DisplayName("verify() 验证所有模块依赖合法、无循环依赖、无非法跨模块访问")
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

    @Nested
    @DisplayName("auth 模块边界")
    class AuthBoundary {

        @Test
        @DisplayName("auth 模块应存在")
        void shouldExist() {
            MODULES.getModuleByName("auth")
                    .orElseThrow(() -> new AssertionError("auth 模块未找到"));
        }

        @Test
        @DisplayName("auth 不应依赖 base / config（allowedDependencies = {}）")
        void shouldNotDependOnBaseOrConfig() {
            ApplicationModule auth = MODULES.getModuleByName("auth")
                    .orElseThrow(() -> new AssertionError("auth 模块未找到"));
            assertModuleNotDependsOn(auth, "base");
            assertModuleNotDependsOn(auth, "config");
        }

        @Test
        @DisplayName("auth 应暴露 'port' 命名接口（AuthorizationPort）")
        void shouldExposePort() {
            assertNamedInterfacePresent("auth", "port");
        }

        @Test
        @DisplayName("auth 应暴露 'event' 命名接口（跨域集成事件）")
        void shouldExposeEvent() {
            assertNamedInterfacePresent("auth", "event");
        }

        @Test
        @DisplayName("auth 应暴露 'security' 命名接口（供 config 装配 SecurityFilterChain）")
        void shouldExposeSecurity() {
            assertNamedInterfacePresent("auth", "security");
        }

        @Test
        @DisplayName("auth 应暴露 'domain-services' 命名接口（SmsService / WechatService 等）")
        void shouldExposeDomainServices() {
            assertNamedInterfacePresent("auth", "domain-services");
        }
    }

    // ============ helpers ============

    @Nested
    @DisplayName("base 模块边界")
    class BaseBoundary {

        @Test
        @DisplayName("base 模块应存在")
        void shouldExist() {
            MODULES.getModuleByName("base")
                    .orElseThrow(() -> new AssertionError("base 模块未找到"));
        }

        @Test
        @DisplayName("base 不应反向依赖 config")
        void shouldNotDependOnConfig() {
            ApplicationModule base = MODULES.getModuleByName("base")
                    .orElseThrow(() -> new AssertionError("base 模块未找到"));
            assertModuleNotDependsOn(base, "config");
        }
    }

    @Nested
    @DisplayName("跨模块依赖契约")
    class CrossModuleContract {

        @Test
        @DisplayName("modules 集合应包含 auth + base 两个有界上下文")
        void shouldContainExpectedModules() {
            List<String> moduleNames = MODULES.stream()
                    .map(ApplicationModule::getName)
                    .toList();
            assertTrue(moduleNames.contains("auth"),
                    "实际模块: " + moduleNames);
            assertTrue(moduleNames.contains("base"),
                    "实际模块: " + moduleNames);
        }
    }
}
