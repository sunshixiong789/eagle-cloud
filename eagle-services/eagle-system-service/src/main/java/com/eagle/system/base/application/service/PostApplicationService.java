package com.eagle.system.base.application.service;

import com.eagle.system.base.application.mapper.PostMapper;
import com.eagle.system.base.domain.model.Post;
import com.eagle.system.base.domain.repository.PostRepository;
import com.eagle.system.base.web.dto.request.CreatePostRequest;
import com.eagle.system.base.web.dto.request.UpdatePostRequest;
import com.eagle.system.base.web.dto.response.PostResponse;
import com.eagle.system.common.exception.SystemErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 岗位管理应用服务
 *
 * @author sunshixiong
 */
@Service
@RequiredArgsConstructor
public class PostApplicationService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    /**
     * 创建岗位
     *
     * @param request 创建岗位请求
     * @return 岗位响应
     */
    @Transactional(rollbackFor = Exception.class)
    public PostResponse createPost(CreatePostRequest request) {
        postRepository.findByPostCode(request.getPostCode())
                .ifPresent(existing -> {
                    throw SystemErrorCode.POST_CODE_EXISTS.toConflictException();
                });

        Post post = Post.create(
                request.getPostCode(),
                request.getPostName(),
                request.getPostSort(),
                request.getRemark()
        );

        Post saved = postRepository.save(post);
        return postMapper.toResponse(saved);
    }

    /**
     * 更新岗位
     *
     * @param id      岗位 ID
     * @param request 更新岗位请求
     * @return 岗位响应
     */
    @Transactional(rollbackFor = Exception.class)
    public PostResponse updatePost(Long id, UpdatePostRequest request) {
        Post post = findPostById(id);

        post.updateInfo(
                request.getPostName(),
                request.getPostSort(),
                request.getRemark()
        );

        Post saved = postRepository.save(post);
        return postMapper.toResponse(saved);
    }

    /**
     * 删除岗位
     *
     * @param id 岗位 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    /**
     * 根据 ID 查询岗位
     *
     * @param id 岗位 ID
     * @return 岗位响应
     */
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        Post post = findPostById(id);
        return postMapper.toResponse(post);
    }

    /**
     * 分页查询岗位列表
     *
     * @param pageable 分页参数
     * @return 岗位响应分页
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> queryPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(postMapper::toResponse);
    }

    /**
     * 启用岗位
     *
     * @param id 岗位 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enablePost(Long id) {
        Post post = findPostById(id);
        post.enable();
        postRepository.save(post);
    }

    /**
     * 禁用岗位
     *
     * @param id 岗位 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void disablePost(Long id) {
        Post post = findPostById(id);
        post.disable();
        postRepository.save(post);
    }

    /**
     * 根据 ID 查找岗位，不存在时抛出异常
     *
     * @param id 岗位 ID
     * @return 岗位实体
     */
    private Post findPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(SystemErrorCode.POST_NOT_FOUND::toNotFoundException);
    }
}
