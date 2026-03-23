package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.MenuApplicationService;
import com.eagle.system.base.web.dto.request.CreateMenuRequest;
import com.eagle.system.base.web.dto.request.UpdateMenuRequest;
import com.eagle.system.base.web.dto.response.MenuResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuApplicationService menuApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MenuResponse createMenu(@Valid @RequestBody CreateMenuRequest request) {
        return menuApplicationService.createMenu(request);
    }

    @PutMapping("/{id}")
    public MenuResponse updateMenu(@PathVariable Long id, @Valid @RequestBody UpdateMenuRequest request) {
        return menuApplicationService.updateMenu(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenu(@PathVariable Long id) {
        menuApplicationService.deleteMenu(id);
    }

    @GetMapping("/{id}")
    public MenuResponse getMenuById(@PathVariable Long id) {
        return menuApplicationService.getMenuById(id);
    }

    @GetMapping
    public Page<MenuResponse> queryMenus(Pageable pageable) {
        return menuApplicationService.queryMenus(pageable);
    }

    @GetMapping("/tree")
    public List<MenuResponse> queryMenuTree() {
        return menuApplicationService.queryMenuTree();
    }

    @PutMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enableMenu(@PathVariable Long id) {
        menuApplicationService.enableMenu(id);
    }

    @PutMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableMenu(@PathVariable Long id) {
        menuApplicationService.disableMenu(id);
    }
}
