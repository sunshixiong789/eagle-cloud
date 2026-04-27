package com.eagle.message.channel;

import com.eagle.message.dto.MessageDTO;
import com.eagle.message.enums.MessageChannelType;
import com.eagle.message.properties.MessageProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * 邮件发送渠道。
 *
 * @author 孙士雄
 */
@Slf4j
@RequiredArgsConstructor
public class EmailMessageChannel implements MessageChannel {

    private final JavaMailSender mailSender;
    private final MessageProperties properties;

    @Override
    public boolean supports(MessageChannelType channelType) {
        return channelType == MessageChannelType.EMAIL;
    }

    @Override
    public void send(MessageDTO message, String renderedContent) {
        MessageProperties.Template template = properties.getTemplates().get(message.templateCode());
        String subject = template != null ? template.getSubject() : "";

        for (String to : message.recipients()) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(properties.getEmail().getFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(renderedContent, true);
                mailSender.send(mimeMessage);
                log.info("Email sent to {}", to);
            } catch (Exception e) {
                log.error("Email send error: to={}, error={}", to, e.getMessage(), e);
            }
        }
    }
}
