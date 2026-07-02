package com.example.tsubuyaki.repository;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.PostLike;
import com.example.tsubuyaki.domain.PostTag;
import com.example.tsubuyaki.domain.Tag;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("h2")
class TagRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Test
    @DisplayName("タグ保存_nameでTagを取得できる")
    void タグ保存_nameでTagを取得できる() {
        tagRepository.save(new Tag("研修"));

        assertThat(tagRepository.findByName("研修"))
                .isPresent()
                .get()
                .extracting(Tag::getName)
                .isEqualTo("研修");
    }

    @Test
    @DisplayName("タグ保存_同一name重複_DB制約で失敗する")
    void タグ保存_同一name重複_Db制約で失敗する() {
        tagRepository.saveAndFlush(new Tag("研修"));

        assertThatThrownBy(() -> tagRepository.saveAndFlush(new Tag("研修")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("投稿タグ保存_投稿に複数タグを関連付けられる")
    void 投稿タグ保存_投稿に複数タグを関連付けられる() {
        Post post = postRepository.save(new Post("alice", "hello #研修 #spring", Instant.parse("2026-05-23T10:00:00Z")));
        Tag training = tagRepository.save(new Tag("研修"));
        Tag spring = tagRepository.save(new Tag("spring"));

        postTagRepository.save(new PostTag(post, training));
        postTagRepository.saveAndFlush(new PostTag(post, spring));

        Post found = postRepository.findById(post.getId()).orElseThrow();
        assertThat(found.getTags()).extracting(Tag::getName)
                .containsExactlyInAnyOrder("研修", "spring");
    }

    @Test
    @DisplayName("投稿タグ保存_同一投稿同一タグ重複_DB制約で失敗する")
    void 投稿タグ保存_同一投稿同一タグ重複_Db制約で失敗する() {
        Post post = postRepository.save(new Post("alice", "hello #研修", Instant.parse("2026-05-23T10:00:00Z")));
        Tag tag = tagRepository.save(new Tag("研修"));
        postTagRepository.saveAndFlush(new PostTag(post, tag));

        assertThatThrownBy(() -> postTagRepository.saveAndFlush(new PostTag(post, tag)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("タグ別一覧_latest_作成日降順で最大50件返す")
    void タグ別一覧_latest_作成日降順で最大50件返す() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Tag other = tagRepository.save(new Tag("other"));
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        IntStream.rangeClosed(1, 51)
                .mapToObj(index -> postRepository.save(new Post(
                        "user" + index,
                        "body" + index,
                        base.plusSeconds(index))))
                .forEach(post -> postTagRepository.save(new PostTag(post, tag)));
        Post otherPost = postRepository.save(new Post("bob", "other", base.plusSeconds(100)));
        postTagRepository.saveAndFlush(new PostTag(otherPost, other));

        List<PostTag> postTags = postTagRepository.findTop50ByTagNameOrderByPostCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));

        assertThat(postTags).hasSize(50);
        assertThat(postTags).extracting(postTag -> postTag.getPost().getBody())
                .startsWith("body51", "body50", "body49")
                .doesNotContain("body1", "other");
    }

    @Test
    @DisplayName("タグ別一覧_latest_ビュー描画時に投稿の基本項目を参照できる")
    void タグ別一覧_latest_ビュー描画時に投稿の基本項目を参照できる() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Post post = postRepository.save(new Post(
                "alice",
                "hello",
                "#e91e63",
                Instant.parse("2026-05-23T00:00:00Z")));
        postTagRepository.saveAndFlush(new PostTag(post, tag));
        entityManager.clear();

        List<PostTag> postTags = postTagRepository.findTop50ByTagNameOrderByPostCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));
        entityManager.clear();

        assertThat(postTags.getFirst().getPost().getAvatarColor()).isEqualTo("#e91e63");
    }

    @Test
    @DisplayName("タグ別一覧_latest_削除済み投稿を含めない")
    void タグ別一覧_latest_削除済み投稿を含めない() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        Post visible = taggedPost("visible", base.plusSeconds(1), tag);
        Post deleted = taggedPost("deleted", base.plusSeconds(2), tag);
        deleted.delete(base.plusSeconds(3));
        postRepository.saveAndFlush(deleted);

        List<PostTag> postTags = postTagRepository.findTop50ByTagNameOrderByPostCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));

        assertThat(postTags).extracting(PostTag::getPost)
                .containsExactly(visible);
    }

    @Test
    @DisplayName("タグ別一覧_popular_いいね数降順で返す")
    void タグ別一覧_popular_いいね数降順で返す() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        Post oneLike = taggedPost("one", base.plusSeconds(1), tag);
        Post threeLikes = taggedPost("three", base.plusSeconds(2), tag);
        like(oneLike, "client01");
        like(threeLikes, "client02");
        like(threeLikes, "client03");
        like(threeLikes, "client04");

        List<PostTag> postTags = postTagRepository.findByTagNameOrderByLikeCountDescCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));

        assertThat(postTags).extracting(postTag -> postTag.getPost().getBody())
                .containsExactly("three", "one");
    }

    @Test
    @DisplayName("タグ別一覧_popular_いいね同数なら作成日降順で返す")
    void タグ別一覧_popular_いいね同数なら作成日降順で返す() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        Post oldPost = taggedPost("old", base.plusSeconds(1), tag);
        Post newPost = taggedPost("new", base.plusSeconds(2), tag);
        like(oldPost, "client01");
        like(newPost, "client02");

        List<PostTag> postTags = postTagRepository.findByTagNameOrderByLikeCountDescCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));

        assertThat(postTags).extracting(postTag -> postTag.getPost().getBody())
                .containsExactly("new", "old");
    }

    @Test
    @DisplayName("タグ別一覧_popular_いいね0件も含め指定タグ以外は含めない")
    void タグ別一覧_popular_いいね0件も含め指定タグ以外は含めない() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Tag other = tagRepository.save(new Tag("other"));
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        Post zeroLike = taggedPost("zero", base.plusSeconds(1), tag);
        Post oneLike = taggedPost("one", base.plusSeconds(2), tag);
        Post otherPost = taggedPost("other", base.plusSeconds(3), other);
        like(oneLike, "client01");
        like(otherPost, "client02");
        postTagRepository.flush();

        List<PostTag> postTags = postTagRepository.findByTagNameOrderByLikeCountDescCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));

        assertThat(postTags).extracting(postTag -> postTag.getPost().getBody())
                .containsExactly("one", "zero")
                .doesNotContain("other");
        assertThat(postTags).extracting(PostTag::getPost)
                .contains(zeroLike);
    }

    @Test
    @DisplayName("タグ別一覧_popular_ビュー描画時に投稿の基本項目を参照できる")
    void タグ別一覧_popular_ビュー描画時に投稿の基本項目を参照できる() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Post post = postRepository.save(new Post(
                "alice",
                "hello",
                "#e91e63",
                Instant.parse("2026-05-23T00:00:00Z")));
        postTagRepository.save(new PostTag(post, tag));
        like(post, "client01");
        postTagRepository.flush();
        entityManager.clear();

        List<PostTag> postTags = postTagRepository.findByTagNameOrderByLikeCountDescCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));
        entityManager.clear();

        assertThat(postTags.getFirst().getPost().getAvatarColor()).isEqualTo("#e91e63");
    }

    @Test
    @DisplayName("タグ別一覧_popular_削除済み投稿を含めない")
    void タグ別一覧_popular_削除済み投稿を含めない() {
        Tag tag = tagRepository.save(new Tag("研修"));
        Instant base = Instant.parse("2026-05-23T00:00:00Z");
        Post visible = taggedPost("visible", base.plusSeconds(1), tag);
        Post deleted = taggedPost("deleted", base.plusSeconds(2), tag);
        deleted.delete(base.plusSeconds(3));
        postRepository.save(deleted);
        like(visible, "client01");
        like(deleted, "client02");
        like(deleted, "client03");
        postTagRepository.flush();

        List<PostTag> postTags = postTagRepository.findByTagNameOrderByLikeCountDescCreatedAtDesc("研修",
                org.springframework.data.domain.PageRequest.of(0, 50));

        assertThat(postTags).extracting(PostTag::getPost)
                .containsExactly(visible);
    }

    private Post taggedPost(String body, Instant createdAt, Tag tag) {
        Post post = postRepository.save(new Post("alice", body, createdAt));
        postTagRepository.save(new PostTag(post, tag));
        return post;
    }

    private void like(Post post, String clientHash) {
        postLikeRepository.save(new PostLike(post, clientHash));
    }
}
