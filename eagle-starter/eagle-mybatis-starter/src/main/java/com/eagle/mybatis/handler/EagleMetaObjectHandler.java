package com.eagle.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MyBatis-Plus 审计字段自动填充处理器。
 *
 * <p>在 INSERT 和 UPDATE 操作时，自动填充以下审计字段：
 * <ul>
 *   <li>INSERT：{@code createTime}、{@code updateTime}、{@code createBy}、{@code updateBy}</li>
 *   <li>UPDATE：{@code updateTime}、{@code updateBy}</li>
 * </ul>
 *
 * <p>当前用户 ID 从 Spring Security 上下文中获取。若获取失败（未登录或上下文不可用），
 * 填充值为 {@code null}，不影响业务主流程。
 *
 * <p>实体类中对应字段需使用 MyBatis-Plus {@code @TableField(fill = FieldFill.INSERT)}
 * 或 {@code @TableField(fill = FieldFill.INSERT_UPDATE)} 注解标记，才会触发自动填充。
 *
 * @author eagle
 */
public class EagleMetaObjectHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(EagleMetaObjectHandler.class);

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = getCurrentUserId();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, currentUserId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = getCurrentUserId();

        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictUpdateFill(metaObject, "updateBy", Long.class, currentUserId);
    }

    /**
     * 从 Spring Security 上下文获取当前登录用户 ID。
     *
     * <p>尝试从 principal 的 {@code id} 属性中提取用户 ID（兼容 JWT claims Map）。
     * 若上下文不可用或解析失败，静默返回 {@code null}，不抛出异常。
     *
     * @return 当前用户 ID，无法获取时返回 {@code null}
     */
    private Long getCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // 尝试从 principal 获取 id 字段（反射，兼容 EagleUser / JWT claims Map）
            if (auth != null && auth.getPrincipal() instanceof Map<?, ?> map) {
                return map.get("id") instanceof Long id ? id : null;
            }
            return null;
        } catch (Exception e) {
            log.debug("[Eagle MyBatis] Failed to get current user id from security context: {}",
                    e.getMessage());
            return null;
        }
    }
}
