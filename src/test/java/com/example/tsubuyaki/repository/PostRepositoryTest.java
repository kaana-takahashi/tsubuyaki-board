package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("投稿一覧_51件以上_新着50件だけを新着順で返す")
    void 投稿一覧_51件以上_新着50件だけを新着順で返す() {
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        IntStream.rangeClosed(1, 51)
                .mapToObj(index -> new Post("user" + index, "body" + index, base.plusSeconds(index)))
                .forEach(postRepository::save);

        List<Post> posts = postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();

        assertThat(posts).hasSize(50);
        assertThat(posts).extracting(Post::getBody)
                .startsWith("body51", "body50", "body49")
                .doesNotContain("body1");
    }

    @Test
    @DisplayName("投稿検索_本文にキーワードを含む投稿だけ新着順で最大50件返す")
    void 投稿検索_本文にキーワードを含む投稿だけ新着順で最大50件返す() {
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        IntStream.rangeClosed(1, 52)
                .mapToObj(index -> new Post("user" + index, "needle body" + index, base.plusSeconds(index)))
                .forEach(postRepository::save);
        postRepository.save(new Post("needle-user", "検索対象外の本文", base.plusSeconds(100)));

        List<Post> posts = postRepository.findTop50ByBodyContainingAndDeletedAtIsNullOrderByCreatedAtDesc("needle");

        assertThat(posts).hasSize(50);
        assertThat(posts).extracting(Post::getBody)
                .startsWith("needle body52", "needle body51", "needle body50")
                .doesNotContain("needle body1", "検索対象外の本文");
    }

    @Test
    @DisplayName("投稿一覧_削除済み投稿_一覧に含めない")
    void 投稿一覧_削除済み投稿_一覧に含めない() {
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        Post visible = postRepository.save(new Post("alice", "visible", base.plusSeconds(1)));
        Post deleted = new Post("bob", "deleted", base.plusSeconds(2));
        deleted.delete(base.plusSeconds(3));
        postRepository.save(deleted);

        List<Post> posts = postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();

        assertThat(posts).containsExactly(visible);
    }

    @Test
    @DisplayName("投稿検索_削除済み投稿_本文に一致しても含めない")
    void 投稿検索_削除済み投稿_本文に一致しても含めない() {
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        Post visible = postRepository.save(new Post("alice", "needle visible", base.plusSeconds(1)));
        Post deleted = new Post("bob", "needle deleted", base.plusSeconds(2));
        deleted.delete(base.plusSeconds(3));
        postRepository.save(deleted);

        List<Post> posts = postRepository.findTop50ByBodyContainingAndDeletedAtIsNullOrderByCreatedAtDesc("needle");

        assertThat(posts).containsExactly(visible);
    }

    @Test
    @DisplayName("投稿保存_avatarColorを保持できる")
    void 投稿保存_avatarColorを保持できる() {
        Post saved = postRepository.save(new Post(
                "alice",
                "hello",
                "#e91e63",
                Instant.parse("2026-05-23T10:00:00Z")));

        Post found = postRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getAvatarColor()).isEqualTo("#e91e63");
    }
}
