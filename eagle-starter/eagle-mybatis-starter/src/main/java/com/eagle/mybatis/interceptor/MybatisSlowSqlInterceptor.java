package com.eagle.mybatis.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;

/**
 * MyBatis 慢 SQL 拦截器。
 *
 * <p>拦截 {@link StatementHandler} 的 {@code query} 和 {@code update} 方法，
 * 记录 SQL 执行耗时。当执行时间超过配置的阈值（{@code slowSqlMillis}）时，
 * 输出 WARN 级别日志，格式为：
 * <pre>
 * [Eagle MyBatis] Slow SQL detected ({}ms > {}ms): {}
 * </pre>
 *
 * <p>SQL 语句从 {@link StatementHandler} 的 {@link BoundSql} 中获取。
 *
 * @author eagle
 */
@Intercepts({
        @Signature(
                type = StatementHandler.class,
                method = "query",
                args = {Statement.class, ResultHandler.class}
        ),
        @Signature(
                type = StatementHandler.class,
                method = "update",
                args = {Statement.class}
        )
})
public class MybatisSlowSqlInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(MybatisSlowSqlInterceptor.class);

    /** 慢 SQL 判断阈值（毫秒） */
    private final long slowSqlMillis;

    /**
     * 创建慢 SQL 拦截器实例。
     *
     * @param slowSqlMillis 慢 SQL 阈值（毫秒），超过此值打印 WARN 日志
     */
    public MybatisSlowSqlInterceptor(long slowSqlMillis) {
        this.slowSqlMillis = slowSqlMillis;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > slowSqlMillis) {
                String sql = extractSql(invocation);
                log.warn("[Eagle MyBatis] Slow SQL detected ({}ms > {}ms): {}",
                        elapsed, slowSqlMillis, sql);
            }
        }
    }

    /**
     * 从 {@link Invocation} 中提取 SQL 语句。
     *
     * <p>通过 {@link StatementHandler} 的 {@link BoundSql} 获取原始 SQL，
     * 提取失败时返回占位字符串，确保主流程不受影响。
     *
     * @param invocation MyBatis 方法调用上下文
     * @return SQL 字符串，提取失败时返回 {@code "[SQL unavailable]"}
     */
    private String extractSql(Invocation invocation) {
        try {
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = statementHandler.getBoundSql();
            return boundSql.getSql().replaceAll("\\s+", " ").trim();
        } catch (Exception e) {
            log.debug("[Eagle MyBatis] Failed to extract SQL from invocation: {}", e.getMessage());
            return "[SQL unavailable]";
        }
    }
}
