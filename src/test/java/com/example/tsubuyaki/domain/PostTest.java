package com.example.tsubuyaki.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    @DisplayName("Post_新規作成直後_deletedAtはnull")
    void Post_新規作成直後_deletedAtはNull() {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z"));

        assertThat(post.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Post_delete_deletedAtに削除日時を設定する")
    void Post_delete_deletedAtに削除日時を設定する() {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z"));
        Instant deletedAt = Instant.parse("2026-05-24T10:00:00Z");

        post.delete(deletedAt);

        assertThat(post.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("Post_delete_削除済みなら最初の削除日時を維持する")
    void Post_delete_削除済みなら最初の削除日時を維持する() {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z"));
        Instant firstDeletedAt = Instant.parse("2026-05-24T10:00:00Z");
        Instant secondDeletedAt = Instant.parse("2026-05-25T10:00:00Z");

        post.delete(firstDeletedAt);
        post.delete(secondDeletedAt);

        assertThat(post.getDeletedAt()).isEqualTo(firstDeletedAt);
    }
}
