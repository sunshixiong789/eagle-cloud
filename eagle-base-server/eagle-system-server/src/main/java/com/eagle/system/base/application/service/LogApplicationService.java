package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.LogMapper;
import com.eagle.system.base.domain.model.SysLog;
import com.eagle.system.base.domain.repository.LogRepository;
import com.eagle.system.base.web.dto.response.LogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogApplicationService {

    private final LogRepository logRepository;
    private final LogMapper logMapper;

    @Transactional(readOnly = true)
    public LogResponse getLogById(Long id) {
        SysLog log = logRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("日志不存在"));
        return logMapper.toResponse(log);
    }

    @Transactional(readOnly = true)
    public Page<LogResponse> queryLogs(Pageable pageable) {
        return logRepository.findAll(pageable).map(logMapper::toResponse);
    }
}
