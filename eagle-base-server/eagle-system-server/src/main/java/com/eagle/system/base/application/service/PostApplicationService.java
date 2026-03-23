package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.PostMapper;
import com.eagle.system.base.domain.model.Post;
import com.eagle.system.base.domain.repository.PostRepository;
import com.eagle.system.base.web.dto.request.CreatePostRequest;
import com.eagle.system.base.web.dto.request.UpdatePostRequest;
import com.eagle.system.base.web.dto.response.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostApplicationService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    @Transactional(rollbackFor = Exception.class)
    public PostResponse createPost(CreatePostRequest request) {
        Post post = Post.create(
                request.getPostCode(),
                request.getPostName(),
                request.getPostSort(),
                request.getRemark()
        );

        Post saved = postRepository.save(post);
        return postMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public PostResponse updatePost(Long id, UpdatePostRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("岗位不存在"));

        post.updateInfo(
                request.getPostName(),
                request.getPostSort(),
                request.getRemark()
        );

        Post saved = postRepository.save(post);
        return postMapper.toResponse(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("岗位不存在"));
        return postMapper.toResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> queryPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(postMapper::toResponse);
    }
}
