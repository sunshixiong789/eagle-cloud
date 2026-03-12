package com.eleganteer.system.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Native Hints 配置
 *
 * @author 孙士雄（sunshix@seeyon.com）
 * 2025/12/10-10:21
 */
@Configuration
@ImportRuntimeHints(NativeHintsConfig.EleganteerRuntimeHints.class)
public class NativeHintsConfig {

    static class EleganteerRuntimeHints implements RuntimeHintsRegistrar {
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // 注册需要反射的资源文件
            hints.resources().registerPattern("messages*.properties");
            hints.resources().registerPattern("templates/**");
        }
    }
}