package com.eagle.message.channel;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.eagle.message.dto.MessageDTO;
import com.eagle.message.enums.MessageChannelType;
import com.eagle.message.properties.MessageProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 阿里云短信发送渠道。
 *
 * @author 孙士雄
 */
@Slf4j
public class SmsMessageChannel implements MessageChannel {

    private final MessageProperties properties;
    private final Client client;

    public SmsMessageChannel(MessageProperties properties) {
        this.properties = properties;
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
        String templateParam = toJson(message.params());
        for (String phone : message.recipients()) {
            try {
                SendSmsRequest request = new SendSmsRequest()
                        .setPhoneNumbers(phone)
                        .setSignName(properties.getSms().getSignName())
                        .setTemplateCode(message.templateCode())
                        .setTemplateParam(templateParam);
                SendSmsResponse response = client.sendSms(request);
                if (!"OK".equals(response.getBody().getCode())) {
                    log.error("SMS send failed: {}", response.getBody().getMessage());
                } else {
                    log.info("SMS sent to {}", phone);
                }
            } catch (Exception e) {
                log.error("SMS send error: phone={}, error={}", phone, e.getMessage(), e);
            }
        }
    }

    private String toJson(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}
