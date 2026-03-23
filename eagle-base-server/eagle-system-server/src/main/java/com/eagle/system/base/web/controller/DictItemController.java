package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.DictItemApplicationService;
import com.eagle.system.base.web.dto.request.CreateDictItemRequest;
import com.eagle.system.base.web.dto.request.UpdateDictItemRequest;
import com.eagle.system.base.web.dto.response.DictItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dict-items")
@RequiredArgsConstructor
public class DictItemController {

    private final DictItemApplicationService dictItemApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DictItemResponse createDictItem(@Valid @RequestBody CreateDictItemRequest request) {
        return dictItemApplicationService.createDictItem(request);
    }

    @PutMapping("/{id}")
    public DictItemResponse updateDictItem(@PathVariable Long id, @Valid @RequestBody UpdateDictItemRequest request) {
        return dictItemApplicationService.updateDictItem(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDictItem(@PathVariable Long id) {
        dictItemApplicationService.deleteDictItem(id);
    }

    @GetMapping("/{id}")
    public DictItemResponse getDictItemById(@PathVariable Long id) {
        return dictItemApplicationService.getDictItemById(id);
    }

    @GetMapping("/tree")
    public List<DictItemResponse> queryDictItemTree(@RequestParam Long dictId) {
        return dictItemApplicationService.queryDictItemsByDictId(dictId);
    }

    @PutMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateDictItem(@PathVariable Long id) {
        dictItemApplicationService.activateDictItem(id);
    }

    @PutMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDictItem(@PathVariable Long id) {
        dictItemApplicationService.deactivateDictItem(id);
    }
}
