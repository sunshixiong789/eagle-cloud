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
 * Spring Modulith 模块边界验证测试(eagle-system-service 拆出 auth 后的拓扑)。
 * <p>
 * 当前模块划分:
 * <pre>
 *   com.eagle.system.base    — 系统管理域(用户、角色、权限、部门、菜单)
 *   com.eagle.system.config  — 全局配置(粘合层)
 *   com.eagle.system.common  — 共享内核(异常体系、基础 DTO)
 *   com.eagle.system.message — 站内消息中心(平台级横切,完全独立)
 *   com.eagle.system.file    — 文件存储(若存在)
 * </pre>
 * <p>
 * auth 已拆分为独立的 eagle-auth-service,base 通过:
 * <ul>
 *   <li>RestClient 同步调用 auth-service 的 /internal/** 端点(在线用户、黑名单)</li>
 *   <li>RocketMQ topic {@code eagle_auth_events} 异步消费集成事件(注册、删除)</li>
 * </ul>
 * 实现解耦,system-service 内部不再有 auth 包。
 *
 * <p><strong>运行</strong>
 * <pre>gradle test --tests "com.eagle.system.ModulithArchitectureTest"</pre>
 */
@DisplayName("Spring Modulith 模块边界验证")
class ModulithArchitectureTest {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(EagleSystemApplication.class);

    /** 断言模块的(直接或传递)依赖中不包含目标模块。 */
    private static void assertModuleNotDependsOn(ApplicationModule module, String forbiddenModule) {
        boolean depends = module.getDependencies(MODULES).stream()
                .map(dep -> dep.getTargetModule().getName())
                .anyMatch(forbiddenModule::equals);
        if (depends) {
            fail(module.getName() + " 不应依赖 " + forbiddenModule + " 模块;"
                    + "请检查 import 语句和 package-info.java 的 allowedDependencies");
        }
    }

    @Test
    @DisplayName("verify() 验证所有模块依赖合法、无循环依赖、无非法跨模块访问")
    void verifiesModularStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("生成模块依赖图文档(PlantUML)")
    void writeDocumentationSnippets() {
        new Documenter(MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

    @Nested
    @DisplayName("base 模块边界")
    class BaseBoundary {

        @Test
        @DisplayName("message 模块应存在")
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
    @DisplayName("message 模块边界(站内消息中心,平台级横切)")
    class MessageBoundary {

        @Test
        @DisplayName("message 模块应存在")
        void shouldExist() {
            MODULES.getModuleByName("message")
                    .orElseThrow(() -> new AssertionError("message 模块未找到"));
        }

        @Test
        @DisplayName("message 应完全独立——不依赖 base / config / file(拆分就绪原则)")
        void shouldBeFullyIsolated() {
            ApplicationModule message = MODULES.getModuleByName("message")
                    .orElseThrow(() -> new AssertionError("message 模块未找到"));
            assertModuleNotDependsOn(message, "base");
            assertModuleNotDependsOn(message, "config");
            assertModuleNotDependsOn(message, "file");
        }
    }

    @Nested
    @DisplayName("跨模块依赖契约")
    class CrossModuleContract {

        @Test
        @DisplayName("modules 集合应包含 base + message,且不应再含 auth(已拆分为独立服务)")
        void shouldContainExpectedModules() {
            List<String> moduleNames = MODULES.stream()
                    .map(ApplicationModule::getName)
                    .toList();
            assertTrue(moduleNames.contains("base"), "实际模块: " + moduleNames);
            assertTrue(moduleNames.contains("message"), "实际模块: " + moduleNames);
            assertTrue(!moduleNames.contains("auth"),
                    "auth 应已拆分到 eagle-auth-service,system-service 不应再包含 auth 模块;实际模块: " + moduleNames);
        }
    }
}
