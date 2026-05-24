package com.eagle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * eagle-auth-service 架构边界验证(防回退)。
 *
 * <p>auth 服务目前仅含 {@code com.eagle.auth} 一个有界上下文,
 * 不适合跑 Spring Modulith 的 {@code ApplicationModules.verify()}(默认按子包推断
 * 会把 DDD 四层视为独立模块并报循环依赖)。本测试改用纯文件扫描,
 * 强制约束 auth 拆分后不允许再 import system 域代码——这是拆分边界的最后一道护栏。
 *
 * <p>当未来 auth 拆出 session / keystore 等子模块、形成多模块结构时,
 * 可改回 {@code ApplicationModules.of(...).verify()}。
 *
 * @author sunshixiong
 */
@DisplayName("Eagle Auth 架构边界验证")
class ModulithArchitectureTest {

    private static final Path SRC_MAIN = Paths.get("src/main/java");

    private static final List<Pattern> FORBIDDEN_IMPORTS = List.of(
            // 拆分后 auth 不允许再依赖 system 内部代码(只能通过 RestClient + 集成事件解耦)
            Pattern.compile("^import\\s+com\\.eagle\\.system\\..*;"),
            // monolith 是单体备份,生产代码不应依赖
            Pattern.compile("^import\\s+com\\.eagle\\.monolith\\..*;"),
            // 网关代码不应被业务服务依赖
            Pattern.compile("^import\\s+com\\.eagle\\.gateway\\..*;")
    );

    @Test
    @DisplayName("auth-service 不应 import 其他服务(system / monolith / gateway)的代码")
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
                "src/main/java 不存在,工作目录是否正确? cwd=" + Paths.get("").toAbsolutePath());
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
