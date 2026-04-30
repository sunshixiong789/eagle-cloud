package com.eagle.message.config;

import com.eagle.message.channel.EmailMessageChannel;
import com.eagle.message.channel.MessageChannel;
import com.eagle.message.channel.SmsMessageChannel;
import com.eagle.message.properties.MessageProperties;
import com.eagle.message.service.NotificationService;
import com.eagle.message.template.MessageTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * 消息通知自动配置。
 *
 * @author 孙士雄
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MessageProperties.class)
@ConditionalOnProperty(prefix = "eagle.message", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessageAutoConfiguration {

    /**
     * 消息发送专用线程池。
     *
     * <p>不使用 common-starter 的通用线程池，避免消息发送（高延迟 IO）占用领域事件处理线程。
     * 配置优雅关闭，防止应用下线时消息丢失。
     */
    @Bean(name = "messageTaskExecutor")
    @ConditionalOnMissingBean(name = "messageTaskExecutor")
    public Executor messageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("message-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待发送中的消息完成后再关闭，防止应用下线时丢失消息
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageTemplateEngine messageTemplateEngine(MessageProperties properties) {
        return new MessageTemplateEngine(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationService notificationService(MessageTemplateEngine templateEngine,
                                                   ObjectProvider<MessageChannel> channelProvider) {
        List<MessageChannel> channels = channelProvider.orderedStream().collect(Collectors.toList());
        return new NotificationService(templateEngine, channels);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "com.aliyun.dysmsapi20170525.Client")
    @ConditionalOnProperty(prefix = "eagle.message.sms", name = "access-key-id")
    static class SmsChannelConfiguration {

        @Bean
        public SmsMessageChannel smsMessageChannel(MessageProperties properties,
                                                   MessageTemplateEngine templateEngine) {
            log.info("SMS message channel enabled, signName: {}", properties.getSms().getSignName());
            return new SmsMessageChannel(properties, templateEngine);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.mail.javamail.JavaMailSender")
    @ConditionalOnBean(JavaMailSender.class)
    static class EmailChannelConfiguration {

        @Bean
        public EmailMessageChannel emailMessageChannel(JavaMailSender mailSender,
                                                       MessageProperties properties,
                                                       MessageTemplateEngine templateEngine) {
            log.info("Email message channel enabled, from: {}", properties.getEmail().getFrom());
            return new EmailMessageChannel(mailSender, properties, templateEngine);
        }
    }
}
