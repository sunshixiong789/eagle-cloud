package com.eagle.message.channel;

import com.eagle.message.channel.sms.SmsProvider;
import com.eagle.message.dto.MessageDTO;
import com.eagle.message.enums.MessageChannelType;
import com.eagle.message.properties.MessageProperties;
import com.eagle.message.template.MessageTemplateEngine;
import lombok.extern.slf4j.Slf4j;

/**
 * 短信发送渠道。
 *
 * <p>本类只负责模板 ID 解析、签名传递与 fan-out 到每个接收号码，
 * 实际 SDK 调用委派给 {@link SmsProvider}（阿里云 / 腾讯云）。
 *
 * <p>服务商通过 {@code eagle.message.sms.provider} 切换；模板 ID 仍写在
 * {@code eagle.message.templates.<code>.sms-template-id}（阿里云 {@code SMS_xxx}，
 * 腾讯云数字字符串）。
 *
 * @author 孙士雄
 */
@Slf4j
public class SmsMessageChannel implements MessageChannel {

    private final MessageProperties properties;
    private final MessageTemplateEngine templateEngine;
    private final SmsProvider provider;

    public SmsMessageChannel(MessageProperties properties,
                             MessageTemplateEngine templateEngine,
                             SmsProvider provider) {
        this.properties = properties;
        this.templateEngine = templateEngine;
        this.provider = provider;
    }

    @Override
    public boolean supports(MessageChannelType channelType) {
        return channelType == MessageChannelType.SMS;
    }

    @Override
    public void send(MessageDTO message, String renderedContent) {
        String smsTemplateId = templateEngine.getSmsTemplateId(message.templateCode());
        String signName = properties.getSms().getSignName();
        for (String phone : message.recipients()) {
            provider.send(phone, smsTemplateId, signName, message.params());
        }
    }
}
