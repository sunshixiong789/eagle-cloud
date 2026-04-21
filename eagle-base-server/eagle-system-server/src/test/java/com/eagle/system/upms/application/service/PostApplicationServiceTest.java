package com.eagle.system.application.service;

import com.eagle.common.exception.ConflictException;
import com.eagle.common.exception.NotFoundException;
import com.eagle.system.application.mapper.PostMapper;
import com.eagle.system.domain.model.Post;
import com.eagle.system.domain.repository.PostRepository;
import com.eagle.system.web.dto.request.CreatePostRequest;
import com.eagle.system.web.dto.request.UpdatePostRequest;
import com.eagle.system.web.dto.response.PostResponse;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PostApplicationService 单元测试
 *
 * @author sunshixiong
 */
@DisplayName("岗位应用服务")
@ExtendWith(MockitoExtension.class)
class PostApplicationServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostMapper postMapper;

    @InjectMocks
    private PostApplicationService postApplicationService;

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("should create post successfully")
        void shouldCreatePostSuccessfully() {
            // Given
            CreatePostRequest request = new CreatePostRequest();
            request.setPostCode("CTO");
            request.setPostName("首席技术官");
            request.setPostSort(1);
            request.setRemark("技术负责人");

            PostResponse expectedResponse = new PostResponse();

            when(postRepository.findByPostCode("CTO")).thenReturn(Optional.empty());
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
            when(postMapper.toResponse(any(Post.class))).thenReturn(expectedResponse);

            // When
            PostResponse result = postApplicationService.createPost(request);

            // Then
            assertNotNull(result);
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("should throw ConflictException when postCode already exists")
        void shouldThrowWhenPostCodeExists() {
            // Given
            CreatePostRequest request = new CreatePostRequest();
            request.setPostCode("CTO");
            request.setPostName("首席技术官");
            request.setPostSort(1);

            Post existingPost = Post.create("CTO", "首席技术官", 1, null);
            when(postRepository.findByPostCode("CTO")).thenReturn(Optional.of(existingPost));

            // When & Then
            assertThrows(ConflictException.class, () ->
                postApplicationService.createPost(request));
            verify(postRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("should update post successfully")
        void shouldUpdatePostSuccessfully() {
            // Given
            Long id = 1L;
            UpdatePostRequest request = new UpdatePostRequest();
            request.setPostName("技术总监");
            request.setPostSort(2);
            request.setRemark("新备注");

            Post existingPost = Post.create("CTO", "首席技术官", 1, "旧备注");
            PostResponse expectedResponse = new PostResponse();

            when(postRepository.findById(id)).thenReturn(Optional.of(existingPost));
            when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
            when(postMapper.toResponse(any(Post.class))).thenReturn(expectedResponse);

            // When
            PostResponse result = postApplicationService.updatePost(id, request);

            // Then
            assertNotNull(result);
            verify(postRepository).save(existingPost);
        }

        @Test
        @DisplayName("should throw NotFoundException when post not found")
        void shouldThrowWhenPostNotFound() {
            // Given
            Long id = 999L;
            UpdatePostRequest request = new UpdatePostRequest();

            when(postRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                postApplicationService.updatePost(id, request));
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("should delete post successfully")
        void shouldDeletePostSuccessfully() {
            // Given
            Long id = 1L;

            // When
            postApplicationService.deletePost(id);

            // Then
            verify(postRepository).deleteById(id);
        }
    }

    @Nested
    @DisplayName("getPostById")
    class GetPostById {

        @Test
        @DisplayName("should return post response when found")
        void shouldReturnPostResponse() {
            // Given
            Long id = 1L;
            Post post = Post.create("CTO", "首席技术官", 1, null);
            PostResponse expectedResponse = new PostResponse();

            when(postRepository.findById(id)).thenReturn(Optional.of(post));
            when(postMapper.toResponse(post)).thenReturn(expectedResponse);

            // When
            PostResponse result = postApplicationService.getPostById(id);

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("should throw NotFoundException when post not found")
        void shouldThrowWhenPostNotFound() {
            // Given
            Long id = 999L;
            when(postRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                postApplicationService.getPostById(id));
        }
    }

    @Nested
    @DisplayName("queryPosts")
    class QueryPosts {

        @Test
        @DisplayName("should return paginated posts")
        void shouldReturnPaginatedPosts() {
            // Given
            Pageable pageable = Pageable.ofSize(10);
            Post post = Post.create("CTO", "首席技术官", 1, null);
            Page<Post> postPage = new PageImpl<>(List.of(post));
            PostResponse response = new PostResponse();

            when(postRepository.findAll(pageable)).thenReturn(postPage);
            when(postMapper.toResponse(post)).thenReturn(response);

            // When
            Page<PostResponse> result = postApplicationService.queryPosts(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("enablePost")
    class EnablePost {

        @Test
        @DisplayName("should enable post successfully")
        void shouldEnablePostSuccessfully() {
            // Given
            Long id = 1L;
            Post post = Post.create("CTO", "首席技术官", 1, null);
            post.disable();

            when(postRepository.findById(id)).thenReturn(Optional.of(post));

            // When
            postApplicationService.enablePost(id);

            // Then
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("should throw NotFoundException when post not found")
        void shouldThrowWhenPostNotFound() {
            // Given
            Long id = 999L;
            when(postRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                postApplicationService.enablePost(id));
        }
    }

    @Nested
    @DisplayName("disablePost")
    class DisablePost {

        @Test
        @DisplayName("should disable post successfully")
        void shouldDisablePostSuccessfully() {
            // Given
            Long id = 1L;
            Post post = Post.create("CTO", "首席技术官", 1, null);

            when(postRepository.findById(id)).thenReturn(Optional.of(post));

            // When
            postApplicationService.disablePost(id);

            // Then
            verify(postRepository).save(post);
        }

        @Test
        @DisplayName("should throw NotFoundException when post not found")
        void shouldThrowWhenPostNotFound() {
            // Given
            Long id = 999L;
            when(postRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(NotFoundException.class, () ->
                postApplicationService.disablePost(id));
        }
    }
}
