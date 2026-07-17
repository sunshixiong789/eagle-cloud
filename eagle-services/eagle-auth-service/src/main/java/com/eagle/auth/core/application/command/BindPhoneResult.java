package com.eagle.auth.core.application.command;

/**
 * 绑定手机号结果。
 *
 * @param merged {@code true} 表示当前影子账号已并入手机号主账号
 *               （影子账号已注销、在线 token 已拉黑），客户端应引导重新登录
 * @author sunshixiong
 */
public record BindPhoneResult(boolean merged) {
}
