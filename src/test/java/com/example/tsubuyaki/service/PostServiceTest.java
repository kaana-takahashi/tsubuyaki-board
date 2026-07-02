package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.PostLike;
import com.example.tsubuyaki.domain.PostTag;
import com.example.tsubuyaki.domain.Tag;
import com.example.tsubuyaki.repository.PostLikeRepository;
import com.example.tsubuyaki.repository.PostRepository;
import com.example.tsubuyaki.repository.PostTagRepository;
import com.example.tsubuyaki.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PostTagRepository postTagRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("投稿一覧_latest_Repositoryの新着50件を返す")
    void 投稿一覧_latest_Repositoryの新着50件を返す() {
        List<Post> expected = List.of(new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z")));
        given(postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(expected);

        List<Post> posts = postService.latest();

        assertThat(posts).isEqualTo(expected);
        verify(postRepository).findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("投稿検索_qあり_本文検索Repositoryを呼び結果を返す")
    void 投稿検索_qあり_本文検索Repositoryを呼び結果を返す() {
        List<Post> expected = List.of(new Post("alice", "hello world", Instant.parse("2026-05-23T10:00:00Z")));
        given(postRepository.findTop50ByBodyContainingAndDeletedAtIsNullOrderByCreatedAtDesc("hello"))
                .willReturn(expected);

        List<Post> posts = postService.list("hello");

        assertThat(posts).isEqualTo(expected);
        verify(postRepository).findTop50ByBodyContainingAndDeletedAtIsNullOrderByCreatedAtDesc("hello");
        verify(postRepository, never()).findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("投稿検索_q空白_latestを返す")
    void 投稿検索_q空白_latestを返す() {
        List<Post> expected = List.of(new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z")));
        given(postRepository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc()).willReturn(expected);

        List<Post> posts = postService.list("   ");

        assertThat(posts).isEqualTo(expected);
        verify(postRepository).findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();
        verify(postRepository, never()).findTop50ByBodyContainingAndDeletedAtIsNullOrderByCreatedAtDesc("   ");
    }

    @Test
    @DisplayName("投稿詳細_findById_Repositoryの検索結果を返す")
    void 投稿詳細_findById_Repositoryの検索結果を返す() {
        Post expected = new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findById(1L)).willReturn(Optional.of(expected));

        Optional<Post> post = postService.findById(1L);

        assertThat(post).contains(expected);
        verify(postRepository).findById(1L);
    }

    @Test
    @DisplayName("投稿登録_create_投稿をデフォルトアバター色でRepositoryへ保存する")
    void 投稿登録_create_投稿をデフォルトアバター色でRepositoryへ保存する() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        Post created = postService.create("alice", "hello");

        Instant after = Instant.now();
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(created).isSameAs(saved);
        assertThat(saved.getAuthor()).isEqualTo("alice");
        assertThat(saved.getBody()).isEqualTo("hello");
        assertThat(saved.getAvatarColor()).isEqualTo("#3498db");
        assertThat(saved.getCreatedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("投稿登録_create_avatarColorつき投稿を保存する")
    void 投稿登録_create_avatarColorつき投稿を保存する() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        postService.create("alice", "hello", "#e91e63");

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getAvatarColor()).isEqualTo("#e91e63");
    }

    @Test
    @DisplayName("投稿登録_create_avatarColor空文字_デフォルト色で保存する")
    void 投稿登録_create_avatarColor空文字_デフォルト色で保存する() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        postService.create("alice", "hello", "   ");

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getAvatarColor()).isEqualTo("#3498db");
    }

    @Test
    @DisplayName("投稿登録_create_avatarColor不正値_デフォルト色で保存する")
    void 投稿登録_create_avatarColor不正値_デフォルト色で保存する() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        postService.create("alice", "hello", "red; color: transparent");

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getAvatarColor()).isEqualTo("#3498db");
    }

    @Test
    @DisplayName("タグ確定_未登録タグを作成して返す")
    void タグ確定_未登録タグを作成して返す() {
        given(tagRepository.findByName("研修")).willReturn(Optional.empty());
        given(tagRepository.save(any(Tag.class))).willAnswer(invocation -> invocation.getArgument(0));

        Tag tag = postService.confirmTag("#研修 ");

        ArgumentCaptor<Tag> tagCaptor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(tagCaptor.capture());
        assertThat(tag).isSameAs(tagCaptor.getValue());
        assertThat(tag.getName()).isEqualTo("研修");
    }

    @Test
    @DisplayName("タグ確定_既存タグは再利用する")
    void タグ確定_既存タグは再利用する() {
        Tag existing = new Tag("研修");
        given(tagRepository.findByName("研修")).willReturn(Optional.of(existing));

        Tag tag = postService.confirmTag("研修");

        assertThat(tag).isSameAs(existing);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("タグ確定_name空_400相当の例外にする")
    void タグ確定_name空_400相当の例外にする() {
        assertThatThrownBy(() -> postService.confirmTag(" # "))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("タグ確定_nameNull_400相当の例外にする")
    void タグ確定_nameNull_400相当の例外にする() {
        assertThatThrownBy(() -> postService.confirmTag(null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("タグ確定_name不正文字_400相当の例外にする")
    void タグ確定_name不正文字_400相当の例外にする() {
        assertThatThrownBy(() -> postService.confirmTag("spring!"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("タグ確定_name51文字_400相当の例外にする")
    void タグ確定_name51文字_400相当の例外にする() {
        assertThatThrownBy(() -> postService.confirmTag("a".repeat(51)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(tagRepository);
    }

    @Test
    @DisplayName("投稿登録_確定済みタグを投稿に関連付ける")
    void 投稿登録_確定済みタグを投稿に関連付ける() {
        Tag existing = new Tag("研修");
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(tagRepository.findByName("研修")).willReturn(Optional.of(existing));

        Post created = postService.create("alice", "今日は研修です", "#3498db", List.of("研修"));

        ArgumentCaptor<PostTag> postTagCaptor = ArgumentCaptor.forClass(PostTag.class);
        verify(postTagRepository).save(postTagCaptor.capture());
        assertThat(postTagCaptor.getValue().getPost()).isSameAs(created);
        assertThat(postTagCaptor.getValue().getTag()).isSameAs(existing);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("投稿登録_同一タグ名は1回だけ関連付ける")
    void 投稿登録_同一タグ名は1回だけ関連付ける() {
        Tag existing = new Tag("研修");
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(tagRepository.findByName("研修")).willReturn(Optional.of(existing));

        postService.create("alice", "資料を共有", "#3498db", List.of("研修", "#研修", " 研修 "));

        verify(tagRepository, times(1)).findByName("研修");
        verify(postTagRepository, times(1)).save(any(PostTag.class));
    }

    @Test
    @DisplayName("投稿登録_tagNamesNull_タグ関連なしで投稿できる")
    void 投稿登録_tagNamesNull_タグ関連なしで投稿できる() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        Post created = postService.create("alice", "タグなし本文", "#3498db", null);

        assertThat(created.getBody()).isEqualTo("タグなし本文");
        verifyNoInteractions(tagRepository, postTagRepository);
    }

    @Test
    @DisplayName("投稿登録_未登録タグ名_投稿タグ関連を作らない")
    void 投稿登録_未登録タグ名_投稿タグ関連を作らない() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(tagRepository.findByName("unknown")).willReturn(Optional.empty());

        postService.create("alice", "hello", "#3498db", List.of("unknown"));

        verify(tagRepository).findByName("unknown");
        verify(postTagRepository, never()).save(any(PostTag.class));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("投稿登録_複数タグ名_入力順で投稿に関連付ける")
    void 投稿登録_複数タグ名_入力順で投稿に関連付ける() {
        Tag java = new Tag("java");
        Tag spring = new Tag("spring");
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(tagRepository.findByName("java")).willReturn(Optional.of(java));
        given(tagRepository.findByName("spring")).willReturn(Optional.of(spring));

        postService.create("alice", "hello", "#3498db", List.of("java", "spring"));

        ArgumentCaptor<PostTag> postTagCaptor = ArgumentCaptor.forClass(PostTag.class);
        verify(postTagRepository, times(2)).save(postTagCaptor.capture());
        assertThat(postTagCaptor.getAllValues())
                .extracting(PostTag::getTag)
                .containsExactly(java, spring);
    }

    @Test
    @DisplayName("投稿登録_本文中のハッシュタグは抽出しない")
    void 投稿登録_本文中のハッシュタグは抽出しない() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        Post created = postService.create("alice", "今日は #研修 です", "#3498db");

        assertThat(created.getBody()).isEqualTo("今日は #研修 です");
        verifyNoInteractions(tagRepository, postTagRepository);
    }

    @Test
    @DisplayName("投稿登録_タグなし本文_従来どおり投稿できる")
    void 投稿登録_タグなし本文_従来どおり投稿できる() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        Post created = postService.create("alice", "タグなし本文", "#3498db");

        assertThat(created.getBody()).isEqualTo("タグなし本文");
        verifyNoInteractions(tagRepository, postTagRepository);
    }

    @Test
    @DisplayName("タグ別一覧_タグ名で関連投稿を新着順に返す")
    void タグ別一覧_タグ名で関連投稿を新着順に返す() {
        Tag tag = new Tag("研修");
        Post first = new Post("alice", "new #研修", Instant.parse("2026-05-23T10:00:00Z"));
        Post second = new Post("bob", "old #研修", Instant.parse("2026-05-23T09:00:00Z"));
        given(postTagRepository.findTop50ByTagNameOrderByPostCreatedAtDesc(any(), any()))
                .willReturn(List.of(new PostTag(first, tag), new PostTag(second, tag)));

        List<Post> posts = postService.listByTag("研修");

        assertThat(posts).containsExactly(first, second);
    }

    @Test
    @DisplayName("タグ別一覧_latest_新着順Repositoryを呼ぶ")
    void タグ別一覧_latest_新着順Repositoryを呼ぶ() {
        Tag tag = new Tag("研修");
        Post post = new Post("alice", "new #研修", Instant.parse("2026-05-23T10:00:00Z"));
        given(postTagRepository.findTop50ByTagNameOrderByPostCreatedAtDesc(any(), any()))
                .willReturn(List.of(new PostTag(post, tag)));

        List<Post> posts = postService.listByTag("研修", "latest");

        assertThat(posts).containsExactly(post);
        verify(postTagRepository).findTop50ByTagNameOrderByPostCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq("研修"),
                org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 50));
        verify(postTagRepository, never()).findByTagNameOrderByLikeCountDescCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("タグ別一覧_popular_人気順Repositoryを呼ぶ")
    void タグ別一覧_popular_人気順Repositoryを呼ぶ() {
        Tag tag = new Tag("研修");
        Post post = new Post("alice", "popular #研修", Instant.parse("2026-05-23T10:00:00Z"));
        given(postTagRepository.findByTagNameOrderByLikeCountDescCreatedAtDesc(any(), any()))
                .willReturn(List.of(new PostTag(post, tag)));

        List<Post> posts = postService.listByTag("研修", "popular");

        assertThat(posts).containsExactly(post);
        verify(postTagRepository).findByTagNameOrderByLikeCountDescCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq("研修"),
                org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 50));
        verify(postTagRepository, never()).findTop50ByTagNameOrderByPostCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("タグ別一覧_sort不正_新着順Repositoryを呼ぶ")
    void タグ別一覧_sort不正_新着順Repositoryを呼ぶ() {
        given(postTagRepository.findTop50ByTagNameOrderByPostCreatedAtDesc(any(), any()))
                .willReturn(List.of());

        List<Post> posts = postService.listByTag("研修", "oldest");

        assertThat(posts).isEmpty();
        verify(postTagRepository).findTop50ByTagNameOrderByPostCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq("研修"),
                org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 50));
        verify(postTagRepository, never()).findByTagNameOrderByLikeCountDescCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("タグ候補_前方一致で最大10件返す")
    void タグ候補_前方一致で最大10件返す() {
        given(tagRepository.findTop10ByNameStartingWithOrderByNameAsc("sp"))
                .willReturn(List.of(new Tag("spring"), new Tag("spring-boot")));

        List<String> suggestions = postService.suggestTagNames("sp");

        assertThat(suggestions).containsExactly("spring", "spring-boot");
    }

    @Test
    @DisplayName("タグ候補_qNull_名前順の最大10件を返す")
    void タグ候補_qNull_名前順の最大10件を返す() {
        given(tagRepository.findTop10ByOrderByNameAsc())
                .willReturn(List.of(new Tag("java"), new Tag("研修")));

        List<String> suggestions = postService.suggestTagNames(null);

        assertThat(suggestions).containsExactly("java", "研修");
        verify(tagRepository).findTop10ByOrderByNameAsc();
        verify(tagRepository, never()).findTop10ByNameStartingWithOrderByNameAsc(any());
    }

    @Test
    @DisplayName("タグ候補_q空白_名前順の最大10件を返す")
    void タグ候補_q空白_名前順の最大10件を返す() {
        given(tagRepository.findTop10ByOrderByNameAsc())
                .willReturn(List.of(new Tag("java"), new Tag("研修")));

        List<String> suggestions = postService.suggestTagNames("   ");

        assertThat(suggestions).containsExactly("java", "研修");
        verify(tagRepository).findTop10ByOrderByNameAsc();
        verify(tagRepository, never()).findTop10ByNameStartingWithOrderByNameAsc(any());
    }

    @Test
    @DisplayName("タグ候補_q前後空白_トリムして前方一致検索する")
    void タグ候補_q前後空白_トリムして前方一致検索する() {
        given(tagRepository.findTop10ByNameStartingWithOrderByNameAsc("sp"))
                .willReturn(List.of(new Tag("spring")));

        List<String> suggestions = postService.suggestTagNames(" sp ");

        assertThat(suggestions).containsExactly("spring");
        verify(tagRepository).findTop10ByNameStartingWithOrderByNameAsc("sp");
    }

    @Test
    @DisplayName("いいねトグル_未いいね_Likeを保存する")
    void いいねトグル_未いいね_Likeを保存する() {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndClientHash(1L, "a1b2c3d4")).willReturn(Optional.empty());
        given(postLikeRepository.save(any(PostLike.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(postLikeRepository.countByPostId(1L)).willReturn(1L);

        long likeCount = postService.toggleLike(1L, "a1b2c3d4");

        ArgumentCaptor<PostLike> captor = ArgumentCaptor.forClass(PostLike.class);
        verify(postLikeRepository).save(captor.capture());
        assertThat(captor.getValue().getPost()).isEqualTo(post);
        assertThat(captor.getValue().getClientHash()).isEqualTo("a1b2c3d4");
        assertThat(likeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("いいねトグル_既いいね_Likeを削除する")
    void いいねトグル_既いいね_Likeを削除する() {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z"));
        PostLike existing = new PostLike(post, "a1b2c3d4");
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        given(postLikeRepository.findByPostIdAndClientHash(1L, "a1b2c3d4")).willReturn(Optional.of(existing));
        given(postLikeRepository.countByPostId(1L)).willReturn(0L);

        long likeCount = postService.toggleLike(1L, "a1b2c3d4");

        verify(postLikeRepository).delete(existing);
        verify(postLikeRepository, never()).save(any(PostLike.class));
        assertThat(likeCount).isEqualTo(0);
    }

    @Test
    @DisplayName("いいねトグル_存在しない投稿id_ResponseStatusExceptionを投げる")
    void いいねトグル_存在しない投稿id_ResponseStatusExceptionを投げる() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.toggleLike(999L, "a1b2c3d4"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verifyNoInteractions(postLikeRepository);
    }

    @Test
    @DisplayName("いいね数_countLikes_Repositoryの件数を返す")
    void いいね数_countLikes_Repositoryの件数を返す() {
        given(postLikeRepository.countByPostId(1L)).willReturn(3L);

        long likeCount = postService.countLikes(1L);

        assertThat(likeCount).isEqualTo(3);
        verify(postLikeRepository).countByPostId(1L);
    }

    @Test
    @DisplayName("投稿削除_deletePost_存在する投稿を論理削除する")
    void 投稿削除_deletePost_存在する投稿を論理削除する() {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:00:00Z"));
        given(postRepository.findById(1L)).willReturn(Optional.of(post));
        Instant before = Instant.now();

        postService.deletePost(1L);

        Instant after = Instant.now();
        assertThat(post.getDeletedAt()).isBetween(before, after);
        verify(postRepository).findById(1L);
        verify(postRepository, never()).delete(post);
        verify(postRepository, never()).deleteById(1L);
    }

    @Test
    @DisplayName("投稿削除_deletePost_存在しない投稿id_404相当の例外にする")
    void 投稿削除_deletePost_存在しない投稿id_404相当の例外にする() {
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(999L))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(postRepository).findById(999L);
        verify(postRepository, never()).deleteById(999L);
    }
}
