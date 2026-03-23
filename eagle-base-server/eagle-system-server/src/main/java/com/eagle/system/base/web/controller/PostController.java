package com.eagle.system.base.web.controller;

import com.eagle.system.base.application.service.PostApplicationService;
import com.eagle.system.base.web.dto.request.CreatePostRequest;
import com.eagle.system.base.web.dto.request.UpdatePostRequest;
import com.eagle.system.base.web.dto.response.PostResponse;
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
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostApplicationService postApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest request) {
        return postApplicationService.createPost(request);
    }

    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id, @Valid @RequestBody UpdatePostRequest request) {
        return postApplicationService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id) {
        postApplicationService.deletePost(id);
    }

    @GetMapping("/{id}")
    public PostResponse getPostById(@PathVariable Long id) {
        return postApplicationService.getPostById(id);
    }

    @GetMapping
    public Page<PostResponse> queryPosts(Pageable pageable) {
        return postApplicationService.queryPosts(pageable);
    }
}
