package com.eagle.message.channel;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.eagle.message.dto.MessageDTO;
import com.eagle.message.enums.MessageChannelType;
import com.eagle.message.properties.MessageProperties;
import com.eagle.message.template.MessageTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 阿里云短信发送渠道。
 *
 * <p>阿里云 SMS 在服务端完成模板渲染，应用层只负责传递参数（{@code templateParam}）。
 * 模板 ID 需在 {@code eagle.message.templates.<code>.sms-template-id} 中配置，
 * 与应用层 {@code templateCode} 分开管理。
 *
 * @author 孙士雄
 */
@Slf4j
public class SmsMessageChannel implements MessageChannel {

    private final MessageProperties properties;
    private final MessageTemplateEngine templateEngine;
    private final Client client;

    public SmsMessageChannel(MessageProperties properties, MessageTemplateEngine templateEngine) {
        this.properties = properties;
        this.templateEngine = templateEngine;
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
    public boolean supports(MessageChannelType channelType) {
        return channelType == MessageChannelType.SMS;
    }

    @Override
    public void send(MessageDTO message, String renderedContent) {
        // 阿里云 SMS 使用在控制台注册的模板 ID，与应用层 templateCode 不同
        String smsTemplateId = templateEngine.getSmsTemplateId(message.templateCode());
        String templateParam = toJson(message.params());

        for (String phone : message.recipients()) {
            try {
                SendSmsRequest request = new SendSmsRequest()
                        .setPhoneNumbers(phone)
                        .setSignName(properties.getSms().getSignName())
                        .setTemplateCode(smsTemplateId)
                        .setTemplateParam(templateParam);
                SendSmsResponse response = client.sendSms(request);
                if (!"OK".equals(response.getBody().getCode())) {
                    log.error("SMS send failed: phone={}, aliyunCode={}, message={}",
                            phone, response.getBody().getCode(), response.getBody().getMessage());
                } else {
                    log.info("SMS sent to {}", phone);
                }
            } catch (Exception e) {
                log.error("SMS send error: phone={}", phone, e);
            }
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