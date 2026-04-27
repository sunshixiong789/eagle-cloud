package com.eagle.message.template;

import com.eagle.message.properties.MessageProperties;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * 消息模板渲染引擎（基于占位符替换）。
 *
 * @author 孙士雄
 */
@RequiredArgsConstructor
public class MessageTemplateEngine {

    private final MessageProperties properties;

    /**
     * 根据模板编码渲染内容。
     *
     * @param templateCode 模板编码
     * @param params       替换参数
     * @return 渲染后的内容
     */
    public String render(String templateCode, Map<String, String> params) {
        MessageProperties.Template template = properties.getTemplates().get(templateCode);
        String content = template != null ? template.getContent() : "";
        if (content == null) {
            content = "";
        }
        for (Map.Entry<String, String> entry : params.entrySet()) {
            content = content.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return content;
    }

    /**
     * 获取模板主题。
     *
     * @param templateCode 模板编码
     * @return 主题
     */
    public String getSubject(String templateCode) {
        MessageProperties.Template template = properties.getTemplates().get(templateCode);
        return template != null ? template.getSubject() : "";
    }
}
