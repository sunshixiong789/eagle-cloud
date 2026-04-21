package com.eagle.system.domain.model;

import com.eagle.system.domain.model.enums.PostStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    @DisplayName("新建岗位默认状态为 ENABLE")
    void shouldDefaultStatusToEnable() {
        Post post = Post.create("PM", "产品经理", 1, null);
        assertThat(post.getStatus()).isEqualTo(PostStatus.ENABLE);
    }

    @Test
    @DisplayName("disable() 将状态置为 DISABLE")
    void shouldDisablePost() {
        Post post = Post.create("PM", "产品经理", 1, null);
        post.disable();
        assertThat(post.getStatus()).isEqualTo(PostStatus.DISABLE);
    }

    @Test
    @DisplayName("enable() 将状态置为 ENABLE")
    void shouldEnablePost() {
        Post post = Post.create("PM", "产品经理", 1, null);
        post.disable();
        post.enable();
        assertThat(post.getStatus()).isEqualTo(PostStatus.ENABLE);
    }
}
