package com.eagle.es.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 高亮处理工具类。
 *
 * <p>将 {@link SearchHit} 中返回的高亮片段（{@code <em>...</em>} 格式）
 * 通过反射替换到目标对象的对应字段中，简化高亮结果的处理流程。
 *
 * <p>此类为无状态工具类，所有方法均为静态方法，禁止实例化。
 *
 * @author eagle
 */
public final class EsHighlightUtil {

    private static final Logger log = LoggerFactory.getLogger(EsHighlightUtil.class);

    /** 高亮片段拼接分隔符 */
    private static final String HIGHLIGHT_SEPARATOR = "...";

    /**
     * 禁止实例化。
     */
    private EsHighlightUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 将 {@link SearchHit} 中的高亮字段替换到对象的对应字段中。
     *
     * <p>遍历高亮字段映射，通过反射找到目标对象中对应名称的字段，
     * 将所有高亮片段（已包含 {@code <em>...</em>} 标记）拼接后设置到字段上。
     * 若字段不存在或类型不匹配，则跳过该字段并记录调试日志。
     *
     * @param <T> 文档类型
     * @param hit Elasticsearch 搜索命中对象，包含高亮信息
     */
    public static <T> void applyHighlight(SearchHit<T> hit) {
        if (hit == null || hit.getContent() == null) {
            return;
        }

        Map<String, List<String>> highlightFields = hit.getHighlightFields();
        if (highlightFields == null || highlightFields.isEmpty()) {
            return;
        }

        T content = hit.getContent();
        Class<?> contentClass = content.getClass();

        for (Map.Entry<String, List<String>> entry : highlightFields.entrySet()) {
            String fieldName = entry.getKey();
            List<String> fragments = entry.getValue();

            if (fragments == null || fragments.isEmpty()) {
                continue;
            }

            // 拼接所有高亮片段
            String highlightedText = String.join(HIGHLIGHT_SEPARATOR, fragments);

            // 通过反射找到目标字段（支持继承层次查找）
            Field field = ReflectionUtils.findField(contentClass, fieldName);
            if (field == null) {
                log.debug("[Eagle ES] Highlight field '{}' not found in class '{}'",
                        fieldName, contentClass.getSimpleName());
                continue;
            }

            // 只替换 String 类型字段
            if (!String.class.equals(field.getType())) {
                log.debug("[Eagle ES] Highlight field '{}' is not String type, skipping", fieldName);
                continue;
            }

            try {
                ReflectionUtils.makeAccessible(field);
                ReflectionUtils.setField(field, content, highlightedText);
            } catch (Exception e) {
                log.warn("[Eagle ES] Failed to apply highlight for field '{}': {}",
                        fieldName, e.getMessage());
            }
        }
    }
}
