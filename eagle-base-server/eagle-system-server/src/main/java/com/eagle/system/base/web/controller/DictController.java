package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.DictApplicationService;
import com.eagle.system.base.web.dto.request.CreateDictRequest;
import com.eagle.system.base.web.dto.request.UpdateDictRequest;
import com.eagle.system.base.web.dto.response.DictResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictApplicationService dictApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DictResponse createDict(@Valid @RequestBody CreateDictRequest request) {
        return dictApplicationService.createDict(request);
    }

    @PutMapping("/{id}")
    public DictResponse updateDict(@PathVariable Long id, @Valid @RequestBody UpdateDictRequest request) {
        return dictApplicationService.updateDict(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDict(@PathVariable Long id) {
        dictApplicationService.deleteDict(id);
    }

    @GetMapping("/{id}")
    public DictResponse getDictById(@PathVariable Long id) {
        return dictApplicationService.getDictById(id);
    }

    @GetMapping
    public Page<DictResponse> queryDict(Pageable pageable) {
        return dictApplicationService.queryDicts(pageable);
    }

    @PutMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateDict(@PathVariable Long id) {
        dictApplicationService.activateDict(id);
    }

    @PutMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDict(@PathVariable Long id) {
        dictApplicationService.deactivateDict(id);
    }
}
