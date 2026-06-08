package com.eagle.payment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * eagle-payment-service 架构边界验证。
 *
 * <p>本服务仅含一个有界上下文 {@code com.eagle.payment.core} (支付收款 / 退款 / 提现 / 对账)。
 * 应用基础包 {@code com.eagle.payment} 只承载 {@code EaglePaymentApplication},因此
 * {@link ApplicationModules#verify()} 会把 {@code core} 识别为单一模块,其下
 * {@code interfaces / application / domain / infrastructure} 视为内部包,
 * 不会触发"四层互依"误报。
 *
 * <p>除 Modulith 标准校验外,本测试附加文件扫描,禁止 import 其他服务
 * (auth / system / monolith / gateway)——payment-service 必须通过 MQ 集成事件 +
 * eagle-restclient-starter 远程调用与其他服务解耦,不允许直接 import。
 *
 * <p><strong>运行</strong>
 * <pre>gradle :eagle-services:eagle-payment-service:test --tests "*.ModulithArchitectureTest"</pre>
 *
 * @author sunshixiong
 */
@DisplayName("Eagle Payment 架构边界验证")
class ModulithArchitectureTest {

    private static final ApplicationModules MODULES =
            ApplicationModules.of(EaglePaymentApplication.class);

    private static final Path SRC_MAIN = Paths.get("src/main/java");

    private static final List<Pattern> FORBIDDEN_IMPORTS = List.of(
            Pattern.compile("^import\\s+(static\\s+)?com\\.eagle\\.auth\\..*;"),
            Pattern.compile("^import\\s+(static\\s+)?com\\.eagle\\.system\\..*;"),
            Pattern.compile("^import\\s+(static\\s+)?com\\.eagle\\.monolith\\..*;"),
            Pattern.compile("^import\\s+(static\\s+)?com\\.eagle\\.gateway\\..*;")
    );

    @Test
    @DisplayName("ApplicationModules.verify() — 模块结构合法,无循环依赖,无非法跨模块访问")
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
    @DisplayName("模块拓扑")
    class Topology {

        @Test
        @DisplayName("只应识别出一个业务模块 core")
        void shouldHaveSingleCoreModule() {
            List<String> moduleNames = MODULES.stream()
                    .map(ApplicationModule::getName)
                    .toList();
            assertTrue(moduleNames.contains("core"),
                    "core 模块未被识别;实际: " + moduleNames);
            assertEquals(1, moduleNames.size(),
                    "payment-service 仅应有一个模块 core,实际: " + moduleNames);
        }
    }

    @Nested
    @DisplayName("跨服务边界(文件扫描)")
    class CrossServiceBoundary {

        @Test
        @DisplayName("payment-service 不应 import 其他服务(auth / system / monolith / gateway)的代码")
        void shouldNotImportOtherServicePackages() throws IOException {
            List<String> violations = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(SRC_MAIN)) {
                stream.filter(p -> p.toString().endsWith(".java"))
                        .forEach(file -> scanFile(file, violations));
            }
            if (!violations.isEmpty()) {
                fail("发现非法跨服务 import:\n  - " + String.join("\n  - ", violations));
            }
        }

        @Test
        @DisplayName("源码根目录应存在")
        void srcMainShouldExist() {
            assertTrue(Files.isDirectory(SRC_MAIN),
                    "src/main/java 不存在,工作目录是否正确? cwd="
                            + Paths.get("").toAbsolutePath());
        }
    }

    private static void scanFile(Path file, List<String> violations) {
        try {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                for (Pattern pattern : FORBIDDEN_IMPORTS) {
                    if (pattern.matcher(line).matches()) {
                        violations.add(file + ":" + (i + 1) + " → " + line);
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to read " + file, e);
        }
    }
}
