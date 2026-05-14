package com.eagle.message.channel.sms;

import java.util.Map;

/**
 * 短信服务商抽象。
 *
 * <p>不同服务商（阿里云、腾讯云等）在 SDK 调用、模板 ID 形态、错误码上差异较大，
 * 通过该接口屏蔽差异，由 {@code SmsMessageChannel} 统一委派。
 *
 * @author 孙士雄
 */
public interface SmsProvider {

    /**
     * 服务商标识，与 {@code eagle.message.sms.provider} 配置值匹配（如 {@code aliyun}、{@code tencent}）。
     *
     * @return 服务商标识
     */
    String name();

    /**
     * 发送单条短信。
     *
     * @param phone      接收手机号（建议为 E.164 格式 {@code +8613800138000}，国内号可只传 11 位由实现按需补全）
     * @param templateId 服务商侧模板 ID（阿里云为 {@code SMS_xxx}，腾讯云为数字字符串）
     * @param signName   短信签名
     * @param params     模板参数（有序 Map，腾讯云按顺序展开为字符串数组）
     */
    void send(String phone, String templateId, String signName, Map<String, String> params);
}
