package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.LogApplicationService;
import com.eagle.system.base.web.dto.response.LogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogApplicationService logApplicationService;

    @GetMapping("/{id}")
    public LogResponse getLogById(@PathVariable Long id) {
        return logApplicationService.getLogById(id);
    }

    @GetMapping
    public Page<LogResponse> queryLogs(Pageable pageable) {
        return logApplicationService.queryLogs(pageable);
    }
}
