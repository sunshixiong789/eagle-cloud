package com.eagle.system.base.domain.repository;

import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.domain.model.enums.LogStatus;
import com.eagle.system.base.domain.model.enums.LogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * 日志 Repository
 *
 * @author sunshixiong
 */
@Repository
public interface LogRepository extends JpaRepository<SysLog, Long>,
        JpaSpecificationExecutor<SysLog> {

    /**
     * 批量删除指定时间之前的日志
     *
     * @param before 截止时间
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM SysLog l WHERE l.createTime < :before")
    int deleteByCreateTimeBefore(LocalDateTime before);

    /**
     * 按 logType 和时间段统计数量
     *
     * @param logType 日志类型
     * @param start   开始时间（含）
     * @param end     结束时间（不含）
     * @return 日志数量
     */
    @Query("SELECT COUNT(l) FROM SysLog l WHERE l.logType = :logType AND l.createTime >= :start AND l.createTime < :end")
    Long countByLogTypeAndPeriod(@Param("logType") LogType logType,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * 按时间段统计总数
     *
     * @param start 开始时间（含）
     * @param end   结束时间（不含）
     * @return 日志总数
     */
    @Query("SELECT COUNT(l) FROM SysLog l WHERE l.createTime >= :start AND l.createTime < :end")
    Long countByPeriod(@Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end);

    /**
     * 今日各 logType 分布（CQRS 投影）
     *
     * @param start 开始时间（含）
     * @param end   结束时间（不含）
     * @return 各类型日志统计列表
     */
    @Query("SELECT l.logType AS logType, COUNT(l) AS count FROM SysLog l " +
            "WHERE l.createTime >= :start AND l.createTime < :end GROUP BY l.logType")
    List<LogTypeSummaryProjection> findLogSummaryByPeriod(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);

    /**
     * 按 logType、status 和时间段统计数量
     *
     * @param logType 日志类型
     * @param status  日志状态
     * @param start   开始时间（含）
     * @param end     结束时间（不含）
     * @return 日志数量
     */
    @Query("SELECT COUNT(l) FROM SysLog l WHERE l.logType = :logType AND l.status = :status " +
            "AND l.createTime >= :start AND l.createTime < :end")
    Long countByLogTypeAndStatusAndPeriod(@Param("logType") LogType logType,
                                          @Param("status") LogStatus status,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /**
     * 按 logType 和时间段统计不重复用户名数
     *
     * @param logType 日志类型
     * @param start   开始时间（含）
     * @param end     结束时间（不含）
     * @return 不重复用户名数
     */
    @Query("SELECT COUNT(DISTINCT l.username) FROM SysLog l WHERE l.logType = :logType " +
            "AND l.createTime >= :start AND l.createTime < :end")
    Long countDistinctUsernameByLogTypeAndPeriod(@Param("logType") LogType logType,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    /**
     * 列表查询（CQRS 读投影）：与 {@link #findAll(Specification, Pageable)} 共用同一组过滤条件，
     * 但只取 {@link LogSummary} 字段，剔除 {@code params / result / exception} 等 TEXT 列。
     *
     * @param spec     动态查询条件
     * @param pageable 分页参数
     * @return 日志摘要投影分页
     */
    default Page<LogSummary> findLogSummariesBy(Specification<SysLog> spec, Pageable pageable) {
        return findBy(spec, query -> query.as(LogSummary.class).page(pageable));
    }

    /**
     * 按日期分组统计登录数（用于趋势图）
     *
     * @param logType 日志类型
     * @param start   开始时间（含）
     * @param end     结束时间（不含）
     * @return 每日登录数投影列表
     */
    @Query("SELECT CAST(l.createTime AS LocalDate) AS date, COUNT(l) AS count " +
            "FROM SysLog l WHERE l.logType = :logType " +
            "AND l.createTime >= :start AND l.createTime < :end " +
            "GROUP BY CAST(l.createTime AS LocalDate) ORDER BY CAST(l.createTime AS LocalDate)")
    List<LoginTrendProjection> findLoginTrendByPeriod(@Param("logType") LogType logType,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    /**
     * 查询用户最近一次指定类型和状态的日志时间。
     *
     * @param username 用户名
     * @param logType  日志类型
     * @param status   日志状态
     * @return 最近日志创建时间
     */
    @Query("SELECT MAX(l.createTime) FROM SysLog l WHERE l.username = :username " +
            "AND l.logType = :logType AND l.status = :status")
    Optional<LocalDateTime> findLatestCreateTimeByUsernameAndLogTypeAndStatus(
            @Param("username") String username,
            @Param("logType") LogType logType,
            @Param("status") LogStatus status);
}
