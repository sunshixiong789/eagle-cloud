package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.MenuMapper;
import com.eagle.system.base.domain.model.Menu;
import com.eagle.system.base.domain.model.enums.MenuType;
import com.eagle.system.base.domain.repository.MenuRepository;
import com.eagle.system.base.web.dto.request.CreateMenuRequest;
import com.eagle.system.base.web.dto.request.UpdateMenuRequest;
import com.eagle.system.base.web.dto.response.MenuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuApplicationService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;

    @Transactional(rollbackFor = Exception.class)
    public MenuResponse createMenu(CreateMenuRequest request) {
        MenuType menuType = MenuType.valueOf(request.getMenuType());
        Menu menu = Menu.create(
                request.getName(),
                request.getEnName(),
                request.getPermission(),
                request.getParentId(),
                request.getIcon(),
                request.getPath(),
                request.getComponent(),
                request.getVisible(),
                request.getSortOrder(),
                menuType
        );

        Menu saved = menuRepository.save(menu);
        return menuMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public MenuResponse updateMenu(Long id, UpdateMenuRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜单不存在"));

        menu.updateInfo(
                request.getName(),
                request.getEnName(),
                request.getPermission(),
                request.getIcon(),
                request.getPath(),
                request.getComponent(),
                request.getVisible(),
                request.getSortOrder()
        );

        Menu saved = menuRepository.save(menu);
        return menuMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        menuRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenuById(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
        return menuMapper.toResponse(menu);
    }

    @Transactional(readOnly = true)
    public Page<MenuResponse> queryMenus(Pageable pageable) {
        return menuRepository.findAll(pageable).map(menuMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> queryMenuTree() {
        List<Menu> allMenus = menuRepository.findAll();
        return buildMenuTree(allMenus, 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enableMenu(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
        menu.enable();
        menuRepository.save(menu);
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableMenu(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
        menu.disable();
        menuRepository.save(menu);
    }

    private List<MenuResponse> buildMenuTree(List<Menu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(menu -> Objects.equals(menu.getParentId(), parentId))
                .sorted(Comparator.comparing(Menu::getSortOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(menu -> {
                    MenuResponse response = menuMapper.toResponse(menu);
                    response.setChildren(buildMenuTree(allMenus, menu.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }
}
