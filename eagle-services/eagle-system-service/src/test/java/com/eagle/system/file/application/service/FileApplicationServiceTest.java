package com.eagle.system.file.application.service;

import com.eagle.common.exception.DomainException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.oss.service.StorageService;
import com.eagle.resource.server.util.SecurityUtils;
import com.eagle.system.file.application.mapper.FileMapper;
import com.eagle.system.file.domain.model.aggregate.FileMetadata;
import com.eagle.system.file.domain.model.enums.FileErrorCode;
import com.eagle.system.file.domain.repository.FileMetadataRepository;
import com.eagle.system.file.infrastructure.config.FileStorageProperties;
import com.eagle.system.file.interfaces.dto.response.FileMetadataResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileApplicationServiceTest {

    private static final Long USER_ID = 1024L;
    private static final String OTHER_USER_ID = "9999";
    private static final String BUCKET = "test-bucket";
    private static final String FAKE_URL = "http://storage/test-bucket/key";

    private FileMetadataRepository fileRepository;
    private StorageService storageService;
    private FileStorageProperties properties;
    private FileApplicationService service;

    private MockedStatic<SecurityUtils> securityMock;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileMetadataRepository.class);
        storageService = Mockito.mock(StorageService.class);
        properties = new FileStorageProperties();
        properties.setBucket(BUCKET);
        properties.setMaxSizeMb(1);
        service = new FileApplicationService(fileRepository, storageService, properties, new FileMapper());

        securityMock = Mockito.mockStatic(SecurityUtils.class);
        securityMock.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
        securityMock.when(() -> SecurityUtils.hasRole(anyString())).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        securityMock.close();
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("should persist metadata and call storage when file is valid")
        void shouldUploadValidFile() {
            MultipartFile file = new MockMultipartFile("file", "report.pdf",
                    "application/pdf", "hello".getBytes());
            when(storageService.upload(eq(BUCKET), anyString(), any(), anyLong(), anyString()))
                    .thenReturn(FAKE_URL);
            when(fileRepository.save(any(FileMetadata.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            FileMetadataResponse response = service.upload(file);

            assertNotNull(response);
            assertEquals("report.pdf", response.originalName());
            assertEquals(5L, response.size());
            assertEquals("application/pdf", response.contentType());
            assertEquals(USER_ID.toString(), response.uploadedBy());

            ArgumentCaptor<FileMetadata> captor = ArgumentCaptor.forClass(FileMetadata.class);
            verify(fileRepository).save(captor.capture());
            FileMetadata saved = captor.getValue();
            assertEquals(BUCKET, saved.getBucket());
            assertEquals("report.pdf", saved.getOriginalName());
            assertEquals(USER_ID.toString(), saved.getUploadedBy());
        }

        @Test
        @DisplayName("should reject empty file")
        void shouldRejectEmpty() {
            MultipartFile empty = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[0]);
            DomainException ex = assertThrows(DomainException.class, () -> service.upload(empty));
            assertEquals(FileErrorCode.FILE_EMPTY.getCode(), ex.getErrorCode().getCode());
            verify(storageService, never()).upload(anyString(), anyString(), any(), anyLong(), anyString());
        }

        @Test
        @DisplayName("should reject oversize file")
        void shouldRejectOversize() {
            byte[] tooLarge = new byte[(int) properties.getMaxSizeBytes() + 1];
            MultipartFile big = new MockMultipartFile("file", "x.pdf", "application/pdf", tooLarge);
            DomainException ex = assertThrows(DomainException.class, () -> service.upload(big));
            assertEquals(FileErrorCode.FILE_TOO_LARGE.getCode(), ex.getErrorCode().getCode());
        }

        @Test
        @DisplayName("should reject extension not in whitelist")
        void shouldRejectUnsupported() {
            MultipartFile exe = new MockMultipartFile("file", "evil.exe",
                    "application/x-msdownload", "x".getBytes());
            DomainException ex = assertThrows(DomainException.class, () -> service.upload(exe));
            assertEquals(FileErrorCode.UNSUPPORTED_FILE_TYPE.getCode(), ex.getErrorCode().getCode());
        }

        @Test
        @DisplayName("should reject path traversal in filename")
        void shouldRejectPathTraversal() {
            MultipartFile evil = new MockMultipartFile("file", "../etc/passwd.txt",
                    "text/plain", "x".getBytes());
            DomainException ex = assertThrows(DomainException.class, () -> service.upload(evil));
            assertEquals(FileErrorCode.INVALID_FILE_NAME.getCode(), ex.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("getMetadata")
    class GetMetadata {

        @Test
        @DisplayName("should return metadata when file exists")
        void shouldReturnMetadata() {
            FileMetadata metadata = FileMetadata.create("default", BUCKET, "default/1024/2026/05/16/abc.pdf",
                    "report.pdf", 100L, "application/pdf", null, USER_ID.toString());
            when(fileRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(metadata));

            FileMetadataResponse response = service.getMetadata(1L);

            assertEquals("report.pdf", response.originalName());
            assertEquals(100L, response.size());
        }

        @Test
        @DisplayName("should throw NotFoundException when missing")
        void shouldThrowNotFound() {
            when(fileRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());
            NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getMetadata(404L));
            assertEquals(FileErrorCode.FILE_NOT_FOUND.getCode(), ex.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("owner can soft-delete own file")
        void ownerCanDelete() {
            FileMetadata metadata = FileMetadata.create("default", BUCKET, "key",
                    "x.pdf", 10L, "application/pdf", null, USER_ID.toString());
            when(fileRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(metadata));
            when(fileRepository.save(any(FileMetadata.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.delete(1L);

            verify(fileRepository).save(metadata);
        }

        @Test
        @DisplayName("admin can soft-delete other's file")
        void adminCanDelete() {
            securityMock.when(() -> SecurityUtils.hasRole("admin")).thenReturn(true);
            FileMetadata metadata = FileMetadata.create("default", BUCKET, "key",
                    "x.pdf", 10L, "application/pdf", null, OTHER_USER_ID);
            when(fileRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(metadata));
            when(fileRepository.save(any(FileMetadata.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            service.delete(1L);

            verify(fileRepository).save(metadata);
        }

        @Test
        @DisplayName("non-owner non-admin cannot delete")
        void otherUserCannotDelete() {
            FileMetadata metadata = FileMetadata.create("default", BUCKET, "key",
                    "x.pdf", 10L, "application/pdf", null, OTHER_USER_ID);
            when(fileRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(metadata));

            DomainException ex = assertThrows(DomainException.class, () -> service.delete(1L));
            assertEquals(FileErrorCode.FILE_ACCESS_DENIED.getCode(), ex.getErrorCode().getCode());
            verify(fileRepository, never()).save(any());
        }
    }
}
