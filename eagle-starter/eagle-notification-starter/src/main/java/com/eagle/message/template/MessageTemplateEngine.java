package com.eagle.message.template;

import com.eagle.message.properties.MessageProperties;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 消息模板渲染引擎（基于 {@code ${key}} 占位符替换）。
 *
 * @author 孙士雄
 */
@RequiredArgsConstructor
public class MessageTemplateEngine {

    private final MessageProperties properties;

    /**
     * 检查模板是否存在。
     *
     * @param templateCode 应用层模板编码
     * @return true 表示模板已配置
     */
    public boolean exists(String templateCode) {
        return properties.getTemplates().containsKey(templateCode);
    }

    /**
     * 渲染模板正文内容。
     *
     * @param templateCode 应用层模板编码
     * @param params       占位符替换参数
     * @return 渲染后的内容；模板不存在时返回空字符串
     */
    public String render(String templateCode, Map<String, String> params) {
        MessageProperties.Template template = properties.getTemplates().get(templateCode);
        String content = template != null ? template.getContent() : "";
        return replacePlaceholders(content, params);
    }

    /**
     * 渲染模板主题（用于邮件等需要主题的渠道）。
     *
     * @param templateCode 应用层模板编码
     * @param params       占位符替换参数
     * @return 渲染后的主题；模板不存在时返回空字符串
     */
    public String renderSubject(String templateCode, Map<String, String> params) {
        MessageProperties.Template template = properties.getTemplates().get(templateCode);
        String subject = template != null ? template.getSubject() : "";
        return replacePlaceholders(subject, params);
    }

    /**
     * 获取模板对应的阿里云 SMS 模板 ID。
     *
     * @param templateCode 应用层模板编码
     * @return 阿里云 SMS 模板 ID；未配置时返回空字符串
     */
    public String getSmsTemplateId(String templateCode) {
        MessageProperties.Template template = properties.getTemplates().get(templateCode);
        return template != null ? template.getSmsTemplateId() : "";
    }

    private String replacePlaceholders(String text, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            text = text.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }
}
