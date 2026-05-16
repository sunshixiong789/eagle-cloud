package com.eagle.message.channel.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.eagle.message.properties.MessageProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 阿里云短信服务商实现。
 *
 * <p>阿里云在服务端完成模板渲染，应用层只需传递参数 JSON；
 * 模板 ID 形如 {@code SMS_123456789}。
 *
 * @author 孙士雄
 */
@Slf4j
public class AliyunSmsProvider implements SmsProvider {

    public static final String NAME = "aliyun";

    private final Client client;

    public AliyunSmsProvider(MessageProperties properties) {
        try {
            Config config = new Config()
                    .setAccessKeyId(properties.getSms().getAccessKeyId())
                    .setAccessKeySecret(properties.getSms().getAccessKeySecret())
                    .setEndpoint(properties.getSms().getEndpoint());
            this.client = new Client(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Aliyun SMS client", e);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void send(String phone, String templateId, String signName, Map<String, String> params) {
        String templateParam = toJson(params);
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateId)
                    .setTemplateParam(templateParam);
            SendSmsResponse response = client.sendSms(request);
            if (!"OK".equals(response.getBody().getCode())) {
                String err = "Aliyun SMS failed: " + response.getBody().getMessage();
                log.error("Aliyun SMS send failed: phone={}, code={}, message={}",
                        phone, response.getBody().getCode(), response.getBody().getMessage());
                throw new RuntimeException(err);
            }
            log.info("Aliyun SMS sent to {}", phone);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Aliyun SMS send error: phone={}", phone, e);
            throw new RuntimeException("Aliyun SMS send error", e);
        }
    }

    /**
     * 将参数 Map 序列化为 JSON 字符串，转义 {@code "} 和 {@code \} 防止格式破坏。
     */
    private String toJson(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey()))
                    .append("\":\"").append(escapeJson(entry.getValue())).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
