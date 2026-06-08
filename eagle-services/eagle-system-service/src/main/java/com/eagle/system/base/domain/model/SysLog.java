package com.eagle.system.base.domain.model;


import com.eagle.datajpa.base.BaseEntity;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.model.enums.LogType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 日志
 *
 * @author sunshixiong
 */

@Getter
@Entity
@Table(name = "sys_log", comment = "系统日志表", indexes = {
        @Index(name = "idx_log_type", columnList = "log_type"),
        @Index(name = "idx_create_time", columnList = "create_time"),
        @Index(name = "idx_user_id", columnList = "user_id")
})
@NoArgsConstructor
public class SysLog extends BaseEntity {

    @NotNull(message = "日志类型不能为空")
    @Column(nullable = false, length = 20, comment = "日志类型")
    @Enumerated(EnumType.STRING)
    private LogType logType;

    @NotBlank(message = "日志标题不能为空")
    @Size(max = 255, message = "日志标题长度不能超过255个字符")
    @Column(nullable = false, length = 255, comment = "日志标题")
    private String title;

    @Column(comment = "用户ID")
    private Long userId;

    @Size(max = 64, message = "用户名长度不能超过64个字符")
    @Column(length = 64, comment = "用户名")
    private String username;

    @Size(max = 50, message = "IP地址长度不能超过50个字符")
    @Column(length = 50, comment = "请求IP地址")
    private String remoteAddr;

    @Size(max = 500, message = "用户代理长度不能超过500个字符")
    @Column(length = 500, comment = "用户代理")
    private String userAgent;

    @Size(max = 500, message = "请求URI长度不能超过500个字符")
    @Column(length = 500, comment = "请求URI")
    private String requestUri;

    @Size(max = 10, message = "请求方法长度不能超过10个字符")
    @Column(length = 10, comment = "请求方法")
    private String method;

    @Column(columnDefinition = "TEXT", comment = "请求参数")
    private String params;

    @Column(columnDefinition = "TEXT", comment = "响应结果")
    private String result;

    @Column(comment = "执行时间(毫秒)")
    private Long time;

    @Column(columnDefinition = "TEXT", comment = "异常信息")
    private String exception;

    @Size(max = 64, message = "服务ID长度不能超过64个字符")
    @Column(length = 64, comment = "服务ID")
    private String serviceId;

    @NotNull(message = "日志状态不能为空")
    @Column(nullable = false, length = 20, comment = "日志状态")
    @Enumerated(EnumType.STRING)
    private LogStatus status;

    private SysLog(LogType logType, String title, Long userId, String username,
                   String remoteAddr, String userAgent, String requestUri, String method,
                   String params, String result, Long time, String exception,
                   String serviceId, LogStatus status) {
        this.logType = logType;
        this.title = title;
        this.userId = userId;
        this.username = username;
        this.remoteAddr = remoteAddr;
        this.userAgent = userAgent;
        this.requestUri = requestUri;
        this.method = method;
        this.params = params;
        this.result = result;
        this.time = time;
        this.exception = exception;
        this.serviceId = serviceId;
        this.status = status;
    }

    /**
     * 创建系统日志记录。
     *
     * @param logType    日志类型
     * @param title      日志标题
     * @param userId     用户 ID
     * @param username   用户名
     * @param remoteAddr 客户端 IP
     * @param userAgent  User-Agent
     * @param requestUri 请求路径
     * @param method     请求方法
     * @param params     请求参数
     * @param result     响应结果
     * @param time       耗时
     * @param exception  异常信息
     * @param serviceId  服务 ID
     * @param status     日志状态
     * @return 系统日志实体
     */
    public static SysLog create(LogType logType, String title, Long userId, String username,
                                String remoteAddr, String userAgent, String requestUri,
                                String method, String params, String result, Long time,
                                String exception, String serviceId, LogStatus status) {
        return new SysLog(logType, title, userId, username, remoteAddr, userAgent, requestUri,
                method, params, result, time, exception, serviceId, status);
    }
}
