/**
 * Auth 模块的仓储命名接口
 * <p>
 * 暴露 {@link com.eagle.system.auth.domain.repository.AccountRepository}，
 * 供 config 模块（SecurityConfig）和 system 模块（AdminInitializer）使用。
 *
 * @author sunshixiong
 */
@NamedInterface("repository")
package com.eagle.system.auth.domain.repository;

import org.springframework.modulith.NamedInterface;