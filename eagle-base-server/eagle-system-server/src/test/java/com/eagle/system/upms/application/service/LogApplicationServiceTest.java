package com.eagle.system.application.service;

import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.application.mapper.LogMapper;
import com.eagle.system.domain.model.SysLog;
import com.eagle.system.domain.repository.LogRepository;
import com.eagle.system.web.dto.request.LogQueryRequest;
import com.eagle.system.web.dto.response.LogResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LogApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("日志应用服务")
@ExtendWith(MockitoExtension.class)
class LogApplicationServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private LogMapper logMapper;

    @InjectMocks
    private LogApplicationService logApplicationService;

    @Nested
    @DisplayName("getLogById")
    class GetLogById {

        @Test
        @DisplayName("should return log response when found")
        void shouldReturnLogResponse() {
            // Given
            Long id = 1L;
            SysLog log = mock(SysLog.class);
            LogResponse expectedResponse = new LogResponse();

            when(logRepository.findById(id)).thenReturn(Optional.of(log));
            when(logMapper.toResponse(log)).thenReturn(expectedResponse);

            // When
            LogResponse result = logApplicationService.getLogById(id);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw NotFoundException when log not found")
        void shouldThrowWhenLogNotFound() {
            // Given
            Long id = 999L;
            when(logRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                logApplicationService.getLogById(id));
        }
    }

    @Nested
    @DisplayName("queryLogs (simple)")
    class QueryLogsSimple {

        @Test
        @DisplayName("should return paginated logs")
        void shouldReturnPaginatedLogs() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            SysLog log = mock(SysLog.class);
            Page<SysLog> logPage = new PageImpl<>(List.of(log));
            LogResponse response = new LogResponse();

            when(logRepository.findAll(pageable)).thenReturn(logPage);
            when(logMapper.toResponse(log)).thenReturn(response);

            // When
            Page<LogResponse> result = logApplicationService.queryLogs(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("queryLogs (with conditions)")
    class QueryLogsWithConditions {

        @Test
        @DisplayName("should return logs matching query conditions")
        @SuppressWarnings("unchecked")
        void shouldReturnLogsMatchingConditions() {
            // Given
            LogQueryRequest request = new LogQueryRequest();
            request.setUsername("admin");
            Pageable pageable = Pageable.ofSize(10);

            SysLog log = mock(SysLog.class);
            Page<SysLog> logPage = new PageImpl<>(List.of(log));
            LogResponse response = new LogResponse();

            when(logRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(logPage);
            when(logMapper.toResponse(log)).thenReturn(response);

            // When
            Page<LogResponse> result = logApplicationService.queryLogs(request, pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("exportLogs")
    class ExportLogs {

        @Test
        @DisplayName("should throw DomainException when exceeding export limit")
        @SuppressWarnings("unchecked")
        void shouldThrowWhenExceedingExportLimit() {
            // Given
            LogQueryRequest request = new LogQueryRequest();
            HttpServletResponse response = mock(HttpServletResponse.class);

            when(logRepository.count(any(Specification.class))).thenReturn(10001L);

            // When & Then
            assertThrows(DomainException.class, () ->
                logApplicationService.exportLogs(request, "xlsx", response));
        }

        @Test
        @DisplayName("should export csv with empty logs")
        @SuppressWarnings("unchecked")
        void shouldExportCsvWithEmptyLogs() throws IOException {
            // Given
            LogQueryRequest request = new LogQueryRequest();
            HttpServletResponse response = mock(HttpServletResponse.class);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(logRepository.count(any(Specification.class))).thenReturn(0L);
            when(response.getWriter()).thenReturn(printWriter);

            // When
            logApplicationService.exportLogs(request, "csv", response);

            // Then
            verify(response).setContentType("text/csv;charset=UTF-8");
        }

        @Test
        @DisplayName("should export xlsx with empty logs")
        @SuppressWarnings("unchecked")
        void shouldExportXlsxWithEmptyLogs() throws IOException {
            // Given
            LogQueryRequest request = new LogQueryRequest();
            HttpServletResponse response = mock(HttpServletResponse.class);
            ServletOutputStream outputStream = mock(ServletOutputStream.class);

            when(logRepository.count(any(Specification.class))).thenReturn(0L);
            when(response.getOutputStream()).thenReturn(outputStream);

            // When
            logApplicationService.exportLogs(request, "xlsx", response);

            // Then
            verify(response).setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }

        @Test
        @DisplayName("should export csv with logs")
        @SuppressWarnings("unchecked")
        void shouldExportCsvWithLogs() throws IOException {
            // Given
            LogQueryRequest request = new LogQueryRequest();
            HttpServletResponse response = mock(HttpServletResponse.class);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            SysLog log = mock(SysLog.class);
            when(log.getId()).thenReturn(1L);
            when(log.getTitle()).thenReturn("登录");
            when(log.getUsername()).thenReturn("admin");

            Page<SysLog> logPage = new PageImpl<>(List.of(log));

            when(logRepository.count(any(Specification.class))).thenReturn(1L);
            when(logRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(logPage);
            when(response.getWriter()).thenReturn(printWriter);

            // When
            logApplicationService.exportLogs(request, "csv", response);

            // Then
            verify(response).setContentType("text/csv;charset=UTF-8");
            String csv = stringWriter.toString();
            assertTrue(csv.contains("ID,类型,标题,用户名,IP,请求URI,状态,执行时间(ms),创建时间"));
        }
    }
}
