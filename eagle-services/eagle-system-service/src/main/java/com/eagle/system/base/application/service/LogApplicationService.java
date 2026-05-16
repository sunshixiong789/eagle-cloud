package com.eagle.system.base.application.service;

import com.eagle.common.exception.codes.DataErrorCode;
import com.eagle.system.base.application.mapper.LogMapper;
import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.domain.repository.LogSpecification;
import com.eagle.system.base.domain.repository.LogSummary;
import com.eagle.system.base.interfaces.dto.request.LogQueryRequest;
import com.eagle.system.base.interfaces.dto.response.LogResponse;
import com.eagle.system.base.domain.model.enums.SystemErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志应用服务
 * <p>
 * 编排日志相关用例，包括日志查询、条件查询等只读操作
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class LogApplicationService {

    private final LogRepository logRepository;
    private final LogMapper logMapper;

    /**
     * 根据 ID 查询日志详情
     *
     * @param id 日志 ID
     * @return 日志响应 DTO
     * @throws com.eagle.common.exception.NotFoundException 日志不存在时抛出
     */
    @Transactional(readOnly = true)
    public LogResponse getLogById(Long id) {
        SysLog log = logRepository.findById(id)
                .orElseThrow(SystemErrorCode.LOG_NOT_FOUND::toNotFoundException);
        return logMapper.toResponse(log);
    }

    /**
     * 分页查询所有日志
     *
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    @Transactional(readOnly = true)
    public Page<LogResponse> queryLogs(Pageable pageable) {
        return logRepository.findAll(pageable).map(logMapper::toResponse);
    }

    /**
     * 条件分页查询日志
     * <p>
     * 支持按日志类型、状态、用户名、请求 URI、IP、创建时间范围进行组合查询
     *
     * @param request  查询条件
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    @Transactional(readOnly = true)
    public Page<LogResponse> queryLogs(LogQueryRequest request, Pageable pageable) {
        Specification<SysLog> spec = buildSpec(request);
        return logRepository.findAll(spec, pageable).map(logMapper::toResponse);
    }

    /**
     * 条件分页查询日志摘要（CQRS 投影）
     * <p>
     * 与 {@link #queryLogs(LogQueryRequest, Pageable)} 共用同一组过滤条件，但只返回列表
     * 展示所需字段（剔除 {@code params / result / exception} 等 TEXT 列），适合大批量列表场景。
     *
     * @param request  查询条件
     * @param pageable 分页参数
     * @return 日志摘要分页结果
     */
    @Transactional(readOnly = true)
    public Page<LogSummary> queryLogSummaries(LogQueryRequest request, Pageable pageable) {
        Specification<SysLog> spec = buildSpec(request);
        return logRepository.findLogSummariesBy(spec, pageable);
    }

    /**
     * 导出日志（xlsx/csv，最多10000条）
     *
     * @param request  查询条件
     * @param format   导出格式（xlsx 或 csv）
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    @Transactional(readOnly = true)
    public void exportLogs(LogQueryRequest request, String format, HttpServletResponse response)
            throws IOException {
        Specification<SysLog> spec = buildSpec(request);
        long totalCount = logRepository.count(spec);

        if (totalCount > 10_000) {
            throw DataErrorCode.EXPORT_LIMIT_EXCEEDED.toDomainException();
        }

        String filename = "sys-log-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if (totalCount == 0) {
            if ("csv".equalsIgnoreCase(format)) {
                exportCsv(List.of(), filename, response);
            } else {
                exportExcel(List.of(), filename, response);
            }
            return;
        }

        Pageable pageable = PageRequest.of(0, (int) totalCount,
                Sort.by(Sort.Direction.DESC, "createTime"));
        List<SysLog> logs = logRepository.findAll(spec, pageable).getContent();

        if ("csv".equalsIgnoreCase(format)) {
            exportCsv(logs, filename, response);
        } else {
            exportExcel(logs, filename, response);
        }
    }

    /**
     * 构建动态查询 Specification
     *
     * @param request 查询条件
     * @return Specification
     */
    private Specification<SysLog> buildSpec(LogQueryRequest request) {
        return Specification
                .where(LogSpecification.logTypeEquals(request.getLogType()))
                .and(LogSpecification.statusEquals(request.getStatus()))
                .and(LogSpecification.usernameLike(request.getUsername()))
                .and(LogSpecification.requestUriLike(request.getRequestUri()))
                .and(LogSpecification.remoteAddrLike(request.getRemoteAddr()))
                .and(LogSpecification.createTimeBetween(
                        request.getStartTime(), request.getEndTime()));
    }

    /**
     * 导出 Excel（xlsx）
     */
    private void exportExcel(List<SysLog> logs, String filename, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + ".xlsx\"");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("日志");
            String[] headers = {"ID", "类型", "标题", "用户名", "IP", "请求URI", "状态",
                    "执行时间(ms)", "创建时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowNum = 1;
            for (SysLog log : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(log.getId() != null ? log.getId() : 0L);
                row.createCell(1).setCellValue(log.getLogType() != null ? log.getLogType().name() : "");
                row.createCell(2).setCellValue(log.getTitle() != null ? log.getTitle() : "");
                row.createCell(3).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(4).setCellValue(log.getRemoteAddr() != null ? log.getRemoteAddr() : "");
                row.createCell(5).setCellValue(log.getRequestUri() != null ? log.getRequestUri() : "");
                row.createCell(6).setCellValue(log.getStatus() != null ? log.getStatus().name() : "");
                row.createCell(7).setCellValue(log.getTime() != null ? log.getTime() : 0L);
                row.createCell(8).setCellValue(
                        log.getCreateTime() != null ? log.getCreateTime().toString() : "");
            }
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        }
    }

    /**
     * 导出 CSV
     */
    private void exportCsv(List<SysLog> logs, String filename, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + ".csv\"");
        try (PrintWriter writer = response.getWriter()) {
            // BOM via writer to avoid OutputStream/Writer conflict
            writer.print('\uFEFF');
            writer.println("ID,类型,标题,用户名,IP,请求URI,状态,执行时间(ms),创建时间");
            for (SysLog log : logs) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        safeStr(log.getId()), safeStr(log.getLogType()),
                        escapeCsv(log.getTitle()), safeStr(log.getUsername()),
                        safeStr(log.getRemoteAddr()), escapeCsv(log.getRequestUri()),
                        safeStr(log.getStatus()), safeStr(log.getTime()),
                        safeStr(log.getCreateTime()));
            }
        }
    }

    /**
     * 安全转字符串，null 返回空串
     */
    private String safeStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    /**
     * CSV 字段转义：含逗号、双引号或换行时包裹双引号
     */
    private String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
