/**
 * Auth 模块的配置契约（{@code @ConfigurationProperties} 绑定类）。
 * <p>
 * 这里只放**配置数据的类型化载体**，不放 {@code @Configuration} 装配类——后者属于
 * 实现细节，留在 {@code core.infrastructure.config}。
 *
 * <p><strong>为什么与四层平级</strong>
 * <p>
 * 配置项是应用的输入契约，不是基础设施实现：同一个 {@code AdminProperties} 既被
 * application 层用例读取，也被 interfaces 层控制器读取。若继续放在
 * {@code core.infrastructure.config}，Controller 读一个配置值就构成
 * interfaces → infrastructure 的分层违例（见 {@code LayeredArchitectureTest}），
 * 而这个违例并不反映真实的架构问题。放在与四层平级的位置，任何层都可以读。
 *
 * <p>新增配置类时：只要有 interfaces 层需要读，就放这里；纯基础设施自用的
 * （如 {@code JwtKeyProperties}、{@code TrustedProxyProperties}）留在原处即可。
 *
 * @author sunshixiong
 */
@NullMarked
package com.eagle.auth.core.config;

import org.jspecify.annotations.NullMarked;
