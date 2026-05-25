package com.eagle.message.channel;

import com.eagle.message.dto.MessageDTO;
import com.eagle.message.enums.MessageChannelType;
import com.eagle.message.properties.MessageProperties;
import com.eagle.message.template.MessageTemplateEngine;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * 邮件发送渠道。
 *
 * <p>使用 {@link MessageTemplateEngine} 渲染邮件正文和主题，
 * 主题中的 {@code ${key}} 占位符同样会被替换。
 *
 * @author eagle
 */
@Slf4j
@RequiredArgsConstructor
public class EmailMessageChannel implements MessageChannel {

    private final JavaMailSender mailSender;
    private final MessageProperties properties;
    private final MessageTemplateEngine templateEngine;

    @Override
    public boolean supports(MessageChannelType channelType) {
        return channelType == MessageChannelType.EMAIL;
    }

    @Override
    public void send(MessageDTO message, String renderedContent) {
        // 主题同样需要走模板引擎渲染，支持 ${key} 占位符
        String subject = templateEngine.renderSubject(message.templateCode(), message.params());

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
                log.error("Email send error: to={}", to, e);
            }
        }
    }
}