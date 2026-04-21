package com.eagle.system.application.service;

import com.eagle.common.exception.NotFoundException;
import com.eagle.system.application.mapper.DeptMapper;
import com.eagle.system.domain.model.Dept;
import com.eagle.system.domain.repository.DeptRepository;
import com.eagle.system.web.dto.request.CreateDeptRequest;
import com.eagle.system.web.dto.request.UpdateDeptRequest;
import com.eagle.system.web.dto.response.DeptResponse;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * DeptApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("部门应用服务")
@ExtendWith(MockitoExtension.class)
class DeptApplicationServiceTest {

    @Mock
    private DeptRepository deptRepository;

    @Mock
    private DeptMapper deptMapper;

    @InjectMocks
    private DeptApplicationService deptApplicationService;

    @Nested
    @DisplayName("createDept")
    class CreateDept {

        @Test
        @DisplayName("should create dept successfully")
        void shouldCreateDeptSuccessfully() {
            // Given
            CreateDeptRequest request = new CreateDeptRequest();
            request.setParentId(null);
            request.setName("技术部");
            request.setLeaderId(1L);
            request.setPhone("13800000000");
            request.setSortOrder(1);

            DeptResponse expectedResponse = new DeptResponse();

            when(deptRepository.save(any(Dept.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deptMapper.toResponse(any(Dept.class))).thenReturn(expectedResponse);

            // When
            DeptResponse result = deptApplicationService.createDept(request);

            // Then
            assertNotNull(result);
            verify(deptRepository).save(any(Dept.class));
        }

        @Test
        @DisplayName("should create child dept with parentId")
        void shouldCreateChildDeptWithParentId() {
            // Given
            CreateDeptRequest request = new CreateDeptRequest();
            request.setParentId(1L);
            request.setName("前端组");
            request.setLeaderId(2L);
            request.setPhone("13900000000");
            request.setSortOrder(1);

            DeptResponse expectedResponse = new DeptResponse();

            when(deptRepository.save(any(Dept.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deptMapper.toResponse(any(Dept.class))).thenReturn(expectedResponse);

            // When
            DeptResponse result = deptApplicationService.createDept(request);

            // Then
            assertNotNull(result);
            verify(deptRepository).save(any(Dept.class));
        }
    }

    @Nested
    @DisplayName("updateDept")
    class UpdateDept {

        @Test
        @DisplayName("should update dept successfully")
        void shouldUpdateDeptSuccessfully() {
            // Given
            Long id = 1L;
            UpdateDeptRequest request = new UpdateDeptRequest();
            request.setName("研发部");
            request.setLeaderId(2L);
            request.setPhone("13900000000");
            request.setSortOrder(2);

            Dept existingDept = Dept.create(null, "技术部", 1L, "13800000000", 1);
            DeptResponse expectedResponse = new DeptResponse();

            when(deptRepository.findById(id)).thenReturn(Optional.of(existingDept));
            when(deptRepository.save(any(Dept.class))).thenAnswer(inv -> inv.getArgument(0));
            when(deptMapper.toResponse(any(Dept.class))).thenReturn(expectedResponse);

            // When
            DeptResponse result = deptApplicationService.updateDept(id, request);

            // Then
            assertNotNull(result);
            assertEquals("研发部", existingDept.getName());
            assertEquals(2L, existingDept.getLeaderId());
            verify(deptRepository).save(existingDept);
        }

        @Test
        @DisplayName("should throw NotFoundException when dept not found")
        void shouldThrowWhenDeptNotFound() {
            // Given
            Long id = 999L;
            UpdateDeptRequest request = new UpdateDeptRequest();

            when(deptRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                deptApplicationService.updateDept(id, request));
        }
    }

    @Nested
    @DisplayName("deleteDept")
    class DeleteDept {

        @Test
        @DisplayName("should delete dept successfully")
        void shouldDeleteDeptSuccessfully() {
            // Given
            Long id = 1L;

            // When
            deptApplicationService.deleteDept(id);

            // Then
            verify(deptRepository).deleteById(id);
        }
    }

    @Nested
    @DisplayName("getDeptById")
    class GetDeptById {

        @Test
        @DisplayName("should return dept response when found")
        void shouldReturnDeptResponse() {
            // Given
            Long id = 1L;
            Dept dept = Dept.create(null, "技术部", 1L, "13800000000", 1);
            DeptResponse expectedResponse = new DeptResponse();

            when(deptRepository.findById(id)).thenReturn(Optional.of(dept));
            when(deptMapper.toResponse(dept)).thenReturn(expectedResponse);

            // When
            DeptResponse result = deptApplicationService.getDeptById(id);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw NotFoundException when dept not found")
        void shouldThrowWhenDeptNotFound() {
            // Given
            Long id = 999L;
            when(deptRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                deptApplicationService.getDeptById(id));
        }
    }

    @Nested
    @DisplayName("queryDepts")
    class QueryDepts {

        @Test
        @DisplayName("should return paginated depts")
        void shouldReturnPaginatedDepts() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            Dept dept = Dept.create(null, "技术部", 1L, "13800000000", 1);
            Page<Dept> deptPage = new PageImpl<>(List.of(dept));
            DeptResponse response = new DeptResponse();

            when(deptRepository.findAll(pageable)).thenReturn(deptPage);
            when(deptMapper.toResponse(dept)).thenReturn(response);

            // When
            Page<DeptResponse> result = deptApplicationService.queryDepts(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("queryDeptTree")
    class QueryDeptTree {

        @Test
        @DisplayName("should return empty tree when no depts")
        void shouldReturnEmptyTreeWhenNoDepts() {
            // Given
            when(deptRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<DeptResponse> result = deptApplicationService.queryDeptTree();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should build tree structure correctly")
        void shouldBuildTreeStructureCorrectly() {
            // Given
            Dept rootDept = Dept.create(null, "公司", null, null, 1);
            setDeptId(rootDept, 1L);

            Dept childDept = Dept.create(1L, "技术部", null, null, 1);
            setDeptId(childDept, 2L);

            DeptResponse rootResponse = new DeptResponse();
            rootResponse.setChildren(new java.util.ArrayList<>());
            DeptResponse childResponse = new DeptResponse();
            childResponse.setChildren(new java.util.ArrayList<>());

            when(deptRepository.findAll()).thenReturn(List.of(rootDept, childDept));
            when(deptMapper.toResponse(rootDept)).thenReturn(rootResponse);
            when(deptMapper.toResponse(childDept)).thenReturn(childResponse);

            // When
            List<DeptResponse> result = deptApplicationService.queryDeptTree();

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1, result.getFirst().getChildren().size());
        }
    }

    @Nested
    @DisplayName("enableDept")
    class EnableDept {

        @Test
        @DisplayName("should enable dept successfully")
        void shouldEnableDeptSuccessfully() {
            // Given
            Long id = 1L;
            Dept dept = Dept.create(null, "技术部", 1L, "13800000000", 1);
            dept.disable();

            when(deptRepository.findById(id)).thenReturn(Optional.of(dept));

            // When
            deptApplicationService.enableDept(id);

            // Then
            verify(deptRepository).save(dept);
        }

        @Test
        @DisplayName("should throw NotFoundException when dept not found")
        void shouldThrowWhenDeptNotFound() {
            // Given
            Long id = 999L;
            when(deptRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                deptApplicationService.enableDept(id));
        }
    }

    @Nested
    @DisplayName("disableDept")
    class DisableDept {

        @Test
        @DisplayName("should disable dept successfully")
        void shouldDisableDeptSuccessfully() {
            // Given
            Long id = 1L;
            Dept dept = Dept.create(null, "技术部", 1L, "13800000000", 1);

            when(deptRepository.findById(id)).thenReturn(Optional.of(dept));

            // When
            deptApplicationService.disableDept(id);

            // Then
            verify(deptRepository).save(dept);
        }

        @Test
        @DisplayName("should throw NotFoundException when dept not found")
        void shouldThrowWhenDeptNotFound() {
            // Given
            Long id = 999L;
            when(deptRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                deptApplicationService.disableDept(id));
        }
    }

    private void setDeptId(Dept dept, Long id) {
        try {
            java.lang.reflect.Field idField = dept.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(dept, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
