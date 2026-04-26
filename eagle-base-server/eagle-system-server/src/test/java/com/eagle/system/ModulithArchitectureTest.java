package com.eagle.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 模块边界验证测试
 * <p>
 * 模块划分（有界上下文）：
 * <pre>
 *   com.eagle.auth    — 认证授权域，定义 AuthorizationPort（六边形架构 Driven Port）
 *   com.eagle.system  — 系统管理域，依赖 auth::port 实现 AuthorizationAdapter
 *   com.eagle.config  — 全局配置（粘合层，引用 auth 安全组件进行装配）
 *   com.eagle.common  — 共享内核（跨域事件契约、异常体系、基类）
 * </pre>
 * <p>
 * <strong>强制约束（verify() 自动检测）</strong>
 * <ul>
 *   <li>auth 不依赖 system（零 system 导入）</li>
 *   <li>system 仅依赖 auth::port（AuthorizationPort 接口和 AuthorizationInfo DTO）</li>
 *   <li>跨域事件在 common.event 中定义</li>
 *   <li>config 仅依赖 auth::security</li>
 * </ul>
 * <p>
 * 运行命令：
 * <pre>gradle test --tests "com.eagle.ModulithArchitectureTest"</pre>
 *
 * @author sunshixiong
 */
@DisplayName("Spring Modulith 模块边界验证")
class ModulithArchitectureTest {

    private static final ApplicationModules modules =
            ApplicationModules.of(EagleSystemApplication.class);

    @Test
    @DisplayName("验证所有模块依赖关系合法，无循环依赖，无非法跨模块访问")
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    @DisplayName("auth 模块应存在且具有 port 命名接口（AuthorizationPort）")
    void authModuleShouldExposePort() {
        var authModule = modules.getModuleByName("auth")
                .orElseThrow(() -> new AssertionError("auth 模块未找到"));

        var namedInterface = authModule.getNamedInterfaces().getByName("port");
        assert namedInterface.isPresent()
                : "auth 模块缺少 'port' 命名接口，请检查 auth/domain/port/package-info.java";
    }

    @Test
    @DisplayName("auth 模块应存在")
    void authModuleShouldExist() {
        modules.getModuleByName("auth")
                .orElseThrow(() -> new AssertionError("auth 模块未找到"));
    }

    @Test
    @DisplayName("生成模块依赖图文档（PlantUML）")
    void writeDocumentationSnippets() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
