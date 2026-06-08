package com.eagle.system.base.application.service;

import com.eagle.common.exception.AppException;
import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.common.exception.codes.DataErrorCode;
import com.eagle.system.base.application.mapper.LogMapper;
import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.domain.model.enums.SystemErrorCode;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.domain.repository.LogSummary;
import com.eagle.system.base.interfaces.dto.request.LogQueryRequest;
import com.eagle.system.base.interfaces.dto.response.LogResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogApplicationServiceTest {

    private static final Long ID = 100L;

    @Mock
    LogRepository logRepository;
    @Mock
    LogMapper logMapper;
    @InjectMocks
    LogApplicationService service;

    @Nested
    @DisplayName("getLogById")
    class GetById {
        @Test
        @DisplayName("应返回Log")
        void shouldReturnLog() {
            SysLog log = new SysLog();
            when(logRepository.findById(ID)).thenReturn(Optional.of(log));
            when(logMapper.toResponse(log)).thenReturn(new LogResponse());
            assertEquals(LogResponse.class, service.getLogById(ID).getClass());
        }

        @Test
        @DisplayName("缺失时应抛出")
        void shouldThrowWhenMissing() {
            when(logRepository.findById(ID)).thenReturn(Optional.empty());
            AppException ex = assertThrows(NotFoundException.class, () -> service.getLogById(ID));
            assertEquals(SystemErrorCode.LOG_NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("queryLogs")
    class QueryLogs {
        @Test
        @DisplayName("应查询普通")
        void shouldQueryPlain() {
            Page<SysLog> page = new PageImpl<>(List.of(new SysLog()));
            when(logRepository.findAll(any(PageRequest.class))).thenReturn(page);
            when(logMapper.toResponse(any(SysLog.class))).thenReturn(new LogResponse());
            assertEquals(1, service.queryLogs(PageRequest.of(0, 10)).getTotalElements());
        }

        @Test
        @DisplayName("应应用Spec")
        @SuppressWarnings("unchecked")
        void shouldApplySpec() {
            Page<SysLog> page = new PageImpl<>(List.of(new SysLog()));
            when(logRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
            when(logMapper.toResponse(any(SysLog.class))).thenReturn(new LogResponse());
            LogQueryRequest req = new LogQueryRequest();
            req.setUsername("alice");
            assertEquals(1, service.queryLogs(req, PageRequest.of(0, 10)).getTotalElements());
        }

        @Test
        @DisplayName("应查询Summaries")
        @SuppressWarnings("unchecked")
        void shouldQuerySummaries() {
            LogSummary summary = org.mockito.Mockito.mock(LogSummary.class);
            Page<LogSummary> page = new PageImpl<>(List.of(summary));
            when(logRepository.findLogSummariesBy(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(page);
            assertEquals(1, service.queryLogSummaries(new LogQueryRequest(), PageRequest.of(0, 10))
                    .getTotalElements());
        }
    }

    @Nested
    @DisplayName("exportLogs")
    class Export {
        @Test
        @DisplayName("TooMany时应抛出")
        @SuppressWarnings("unchecked")
        void shouldThrowWhenTooMany() {
            when(logRepository.count(any(Specification.class))).thenReturn(10_001L);
            HttpServletResponse resp = new MockHttpServletResponse();
            AppException ex = assertThrows(DomainException.class,
                    () -> service.exportLogs(new LogQueryRequest(), "xlsx", resp));
            assertEquals(DataErrorCode.EXPORT_LIMIT_EXCEEDED, ex.getErrorCode());
        }

        @Test
        @DisplayName("应写入空Xlsx")
        @SuppressWarnings("unchecked")
        void shouldWriteEmptyXlsx() throws Exception {
            when(logRepository.count(any(Specification.class))).thenReturn(0L);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            service.exportLogs(new LogQueryRequest(), "xlsx", resp);
            assertTrue(resp.getContentAsByteArray().length > 0);
            assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    resp.getContentType());
        }
    }
}
