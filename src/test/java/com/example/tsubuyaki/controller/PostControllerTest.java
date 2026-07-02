package com.example.tsubuyaki.controller;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.Tag;
import com.example.tsubuyaki.service.PostService;
import com.example.tsubuyaki.web.dto.PostForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PostController.class)
class PostControllerTest {

    private TimeZone defaultTimeZone;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @BeforeEach
    void setUpTimeZone() {
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    @AfterEach
    void tearDownTimeZone() {
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    @DisplayName("投稿一覧_投稿0件_空メッセージを表示する")
    void 投稿一覧_投稿0件_空メッセージを表示する() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("posts", Collections.emptyList()))
                .andExpect(content().string(containsString("まだ投稿はありません")));
    }

    @Test
    @DisplayName("投稿一覧_宮崎テーマの共通レイアウトを適用する")
    void 投稿一覧_宮崎テーマの共通レイアウトを適用する() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<body class=\"miyazaki-shell\">")));
    }

    @Test
    @DisplayName("投稿一覧_qあり_検索結果をposts_listへ渡す")
    void 投稿一覧_qあり_検索結果をpostsListへ渡す() throws Exception {
        Post post = new Post("alice", "hello world", Instant.parse("2026-05-23T10:15:00Z"));
        given(postService.list("hello")).willReturn(List.of(post));

        mockMvc.perform(get("/posts").param("q", "hello"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("posts", List.of(post)));
    }

    @Test
    @DisplayName("投稿一覧_qあり_qをmodelに保持する")
    void 投稿一覧_qあり_qをmodelに保持する() throws Exception {
        given(postService.list("hello")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts").param("q", "hello"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("q", "hello"));
    }

    @Test
    @DisplayName("投稿一覧_更新ボタン_押すとpostsスラッシュへGETリクエストする")
    void 投稿一覧_更新ボタン_押すとpostsスラッシュへGetリクエストする() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<form class=\"refresh-form\" action=\"/posts/\" method=\"get\">")))
                .andExpect(content().string(containsString("<button type=\"submit\">更新</button>")));
    }

    @Test
    @DisplayName("投稿一覧_検索フォーム_GET_postsへqを送信できる")
    void 投稿一覧_検索フォーム_GetPostsへqを送信できる() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<form class=\"search-form\" action=\"/posts\" method=\"get\">\\s*"
                        + "<input[^>]+type=\"search\"[^>]+name=\"q\"[^>]*>\\s*"
                        + "<button type=\"submit\">検索</button>\\s*</form>.*")));
    }

    @Test
    @DisplayName("投稿一覧_検索後_入力欄にqを保持する")
    void 投稿一覧_検索後_入力欄にqを保持する() throws Exception {
        given(postService.list("hello")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts").param("q", "hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]+type=\"search\"[^>]+name=\"q\""
                        + "[^>]+value=\"hello\"[^>]*>.*")));
    }

    @Test
    @DisplayName("タグ別一覧_GET_tags_name_関連投稿をposts_listへ渡す")
    void タグ別一覧_GetTagsName_関連投稿をPostsListへ渡す() throws Exception {
        Post post = new Post("alice", "hello #研修", Instant.parse("2026-05-23T10:15:00Z"));
        given(postService.listByTag("研修", "latest")).willReturn(List.of(post));

        mockMvc.perform(get("/tags/研修"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("posts", List.of(post)))
                .andExpect(model().attribute("tagName", "研修"))
                .andExpect(model().attribute("sort", "latest"));
    }

    @Test
    @DisplayName("タグ別一覧_存在しないタグ_空一覧を表示する")
    void タグ別一覧_存在しないタグ_空一覧を表示する() throws Exception {
        given(postService.listByTag("unknown", "latest")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/unknown"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attribute("posts", Collections.emptyList()))
                .andExpect(model().attribute("tagName", "unknown"))
                .andExpect(content().string(containsString("まだ投稿はありません")));
    }

    @Test
    @DisplayName("タグ別一覧_sort未指定_latestでServiceを呼ぶ")
    void タグ別一覧_sort未指定_latestでServiceを呼ぶ() throws Exception {
        given(postService.listByTag("研修", "latest")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/研修"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "latest"));

        then(postService).should().listByTag("研修", "latest");
    }

    @Test
    @DisplayName("タグ別一覧_sort_latest_latestでServiceを呼ぶ")
    void タグ別一覧_sort_latest_latestでServiceを呼ぶ() throws Exception {
        given(postService.listByTag("研修", "latest")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/研修").param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "latest"));

        then(postService).should().listByTag("研修", "latest");
    }

    @Test
    @DisplayName("タグ別一覧_sort_popular_popularでServiceを呼ぶ")
    void タグ別一覧_sort_popular_popularでServiceを呼ぶ() throws Exception {
        given(postService.listByTag("研修", "popular")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/研修").param("sort", "popular"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "popular"));

        then(postService).should().listByTag("研修", "popular");
    }

    @Test
    @DisplayName("タグ別一覧_sort不正_latestとして扱う")
    void タグ別一覧_sort不正_latestとして扱う() throws Exception {
        given(postService.listByTag("研修", "latest")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/研修").param("sort", "oldest"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "latest"));

        then(postService).should().listByTag("研修", "latest");
    }

    @Test
    @DisplayName("タグ別一覧_タイトルと見出しにタグ名を表示する")
    void タグ別一覧_タイトルと見出しにタグ名を表示する() throws Exception {
        given(postService.listByTag("研修", "latest")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/研修"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>#研修 - 社内つぶやきボード</title>")))
                .andExpect(content().string(containsString("<h1>#研修</h1>")));
    }

    @Test
    @DisplayName("タグ別一覧_並び替えリンクを表示する")
    void タグ別一覧_並び替えリンクを表示する() throws Exception {
        given(postService.listByTag("spring", "popular")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/spring").param("sort", "popular"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<a href=\"/tags/spring?sort=latest\"")))
                .andExpect(content().string(containsString(">最新ポスト</a>")))
                .andExpect(content().string(containsString("<a href=\"/tags/spring?sort=popular\"")))
                .andExpect(content().string(containsString(">人気ポスト</a>")));
    }

    @Test
    @DisplayName("タグ別一覧_検索フォームと更新ボタンを表示しない")
    void タグ別一覧_検索フォームと更新ボタンを表示しない() throws Exception {
        given(postService.listByTag("spring", "latest")).willReturn(Collections.emptyList());

        mockMvc.perform(get("/tags/spring"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("class=\"search-form\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("class=\"refresh-form\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("本文を検索"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">更新</button>"))));
    }

    @Test
    @DisplayName("投稿一覧_通常表示_タグ並び替えリンクを表示しない")
    void 投稿一覧_通常表示_タグ並び替えリンクを表示しない() throws Exception {
        given(postService.latest()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("最新ポスト"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("人気ポスト"))));
    }

    @Test
    @DisplayName("タグ候補_GET_tags_suggestions_前方一致のタグ名を返す")
    void タグ候補_GetTagsSuggestions_前方一致のタグ名を返す() throws Exception {
        given(postService.suggestTagNames("sp")).willReturn(List.of("spring", "spring-boot"));

        mockMvc.perform(get("/tags/suggestions").param("q", "sp"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"spring\",\"spring-boot\"]"));
    }

    @Test
    @DisplayName("タグ候補_GET_tags_suggestions_q空_タグ名を最大10件返す")
    void タグ候補_GetTagsSuggestions_Q空_タグ名を最大10件返す() throws Exception {
        given(postService.suggestTagNames("")).willReturn(List.of("java", "研修"));

        mockMvc.perform(get("/tags/suggestions").param("q", ""))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"java\",\"研修\"]"));
    }

    @Test
    @DisplayName("タグ確定_POST_tags_未登録タグを作成してJSONを返す")
    void タグ確定_PostTags_未登録タグを作成してJsonを返す() throws Exception {
        given(postService.confirmTag("spring")).willReturn(new Tag("spring"));

        mockMvc.perform(post("/tags").param("name", "spring"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"spring\"}"));
    }

    @Test
    @DisplayName("タグ確定_POST_tags_既存タグを再利用してJSONを返す")
    void タグ確定_PostTags_既存タグを再利用してJsonを返す() throws Exception {
        given(postService.confirmTag("#研修")).willReturn(new Tag("研修"));

        mockMvc.perform(post("/tags").param("name", "#研修"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"研修\"}"));
    }

    @Test
    @DisplayName("タグ確定_POST_tags_name空_400を返す")
    void タグ確定_PostTags_Name空_400を返す() throws Exception {
        given(postService.confirmTag(""))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

        mockMvc.perform(post("/tags").param("name", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_投稿者内容投稿日の順に表示する")
    void 投稿一覧_投稿あり_投稿者内容投稿日の順に表示する() throws Exception {
        Post post = new Post(
                "alice",
                "長い本文でも読みやすく折り返して表示する投稿です。",
                Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<article class=\"post post--linkable\">\\s*"
                        + "<a class=\"post__detail-link\"\\s+href=\"/posts/1\""
                        + " aria-label=\"alice の投稿詳細を表示\"></a>\\s*"
                        + ".*<div class=\"post__author\">.*</div>\\s*"
                        + "<p class=\"post__body\">長い本文でも読みやすく折り返して表示する投稿です。</p>\\s*"
                        + "<div class=\"post__meta\">\\s*"
                        + "<time class=\"post__created-at\".*>2026-05-23 19:15</time>\\s*</div>.*")));
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_投稿者名の左に頭文字アバターを表示する")
    void 投稿一覧_投稿あり_投稿者名の左に頭文字アバターを表示する() throws Exception {
        Post post = new Post("alice", "hello", "#e91e63", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<div class=\"post__author\">\\s*"
                        + "<span class=\"post__avatar\" style=\"background-color: #e91e63\">A</span>\\s*"
                        + "<span class=\"post__author-name\">alice</span>\\s*</div>.*")));
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_投稿カード全体が詳細リンクになる")
    void 投稿一覧_投稿あり_投稿カード全体が詳細リンクになる() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<article class=\"post post--linkable\">\\s*"
                        + "<a class=\"post__detail-link\"\\s+href=\"/posts/1\""
                        + " aria-label=\"alice の投稿詳細を表示\"></a>.*</article>.*")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">詳細</a>"))));
    }

    @Test
    @DisplayName("投稿一覧_タグあり_タグリンクを本文の下かつ投稿日上に表示する")
    void 投稿一覧_タグあり_タグリンクを本文の下かつ投稿日上に表示する() throws Exception {
        Post post = new Post("alice", "hello #spring", Instant.parse("2026-05-23T10:15:00Z"));
        post.addTag(new Tag("spring"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<p class=\"post__body\">hello #spring</p>\\s*"
                        + "<div class=\"post__tags\">\\s*"
                        + "<a[^>]+href=\"/tags/spring\"[^>]*>#spring</a>\\s*</div>\\s*"
                        + "<div class=\"post__meta\">\\s*"
                        + "<time class=\"post__created-at\".*>2026-05-23 19:15</time>.*")));
    }

    @Test
    @DisplayName("投稿一覧_タグなし_タグリンク領域を表示しない")
    void 投稿一覧_タグなし_タグリンク領域を表示しない() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("post__tags"))));
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_三点メニューと削除フォームを表示する")
    void 投稿一覧_投稿あり_三点メニューと削除フォームを表示する() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<div class=\"post__actions\">\\s*"
                        + "<button class=\"post__menu-button\"\\s+type=\"button\""
                        + "\\s+aria-label=\"投稿メニューを開く\"\\s+aria-haspopup=\"true\""
                        + "\\s+aria-expanded=\"false\">"
                        + ".*</button>\\s*"
                        + "<div class=\"post__menu\" hidden(?:=\"hidden\")?>\\s*"
                        + "<form class=\"post__delete-form\" action=\"/posts/1/delete\" method=\"post\">.*"
                        + "<button class=\"post__delete-button\" type=\"submit\">削除</button>\\s*"
                        + "</form>\\s*</div>\\s*</div>.*")));
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_三点メニュー追加後も投稿カード全体リンクを維持する")
    void 投稿一覧_投稿あり_三点メニュー追加後も投稿カード全体リンクを維持する() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"post__detail-link\"")))
                .andExpect(content().string(containsString("href=\"/posts/1\"")));
    }

    @Test
    @DisplayName("投稿一覧_投稿あり_三点メニュー開閉スクリプトを読み込む")
    void 投稿一覧_投稿あり_三点メニュー開閉スクリプトを読み込む() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.latest()).willReturn(List.of(post));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<script[^>]+src=\"/js/post-menu.js\""
                        + "[^>]+defer(?:=\"defer\")?[^>]*></script>.*")));
    }

    @Test
    @DisplayName("投稿詳細_存在するid_posts_detailビューに投稿を渡す")
    void 投稿詳細_存在するid_posts_detailビューに投稿を渡す() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/detail"))
                .andExpect(model().attribute("post", post))
                .andExpect(content().string(containsString("<h1>投稿詳細</h1>")))
                .andExpect(content().string(containsString("<a href=\"/posts\">一覧に戻る</a>")))
                .andExpect(content().string(matchesPattern("(?s).*<article class=\"post\">\\s*"
                        + "<div class=\"post__author\">.*</div>\\s*"
                        + "<p class=\"post__body\">hello</p>\\s*"
                        + ".*<time class=\"post__created-at\".*>2026-05-23 19:15</time>.*")));
    }

    @Test
    @DisplayName("投稿詳細_宮崎テーマの共通レイアウトを適用する")
    void 投稿詳細_宮崎テーマの共通レイアウトを適用する() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<body class=\"miyazaki-shell\">")));
    }

    @Test
    @DisplayName("投稿詳細_投稿者名の左に頭文字アバターを表示する")
    void 投稿詳細_投稿者名の左に頭文字アバターを表示する() throws Exception {
        Post post = new Post("kaana", "hello", "#3498db", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<div class=\"post__author\">\\s*"
                        + "<span class=\"post__avatar\" style=\"background-color: #3498db\">K</span>\\s*"
                        + "<span class=\"post__author-name\">kaana</span>\\s*</div>.*")));
    }

    @Test
    @DisplayName("投稿詳細_タグリンクを表示する")
    void 投稿詳細_タグリンクを表示する() throws Exception {
        Post post = new Post("alice", "hello #spring", Instant.parse("2026-05-23T10:15:00Z"));
        post.addTag(new Tag("spring"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<div class=\"post__tags\">\\s*"
                        + "<a[^>]+href=\"/tags/spring\"[^>]*>#spring</a>\\s*</div>.*")));
    }

    @Test
    @DisplayName("投稿詳細_関連タグだけをタグリンクとして表示する")
    void 投稿詳細_関連タグだけをタグリンクとして表示する() throws Exception {
        Post post = new Post("alice", "hello #spring world", Instant.parse("2026-05-23T10:15:00Z"));
        post.addTag(new Tag("spring"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<p class=\"post__body\">hello #spring world</p>")))
                .andExpect(content().string(matchesPattern("(?s).*<div class=\"post__tags\">\\s*"
                        + "<a[^>]+href=\"/tags/spring\"[^>]*>#spring</a>\\s*</div>.*")));
    }

    @Test
    @DisplayName("投稿詳細_タグなし_タグリンク領域を表示しない")
    void 投稿詳細_タグなし_タグリンク領域を表示しない() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("post__tags"))));
    }

    @Test
    @DisplayName("投稿詳細_存在するid_likeCountをmodelに渡す")
    void 投稿詳細_存在するid_likeCountをmodelに渡す() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));
        given(postService.countLikes(1L)).willReturn(1312L);

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/detail"))
                .andExpect(model().attribute("likeCount", 1312L));
    }

    @Test
    @DisplayName("投稿詳細_いいねボタン_本文の下かつ作成日の上に表示する")
    void 投稿詳細_いいねボタン_本文の下かつ作成日の上に表示する() throws Exception {
        Post post = new Post("alice", "hello", Instant.parse("2026-05-23T10:15:00Z"));
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postService.findById(1L)).willReturn(Optional.of(post));
        given(postService.countLikes(1L)).willReturn(1312L);

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<p class=\"post__body\">hello</p>\\s*"
                        + "<form class=\"post__like-form\" action=\"/posts/1/likes\" method=\"post\">.*"
                        + "<button[^>]*class=\"post__like-button\"[^>]*>\\s*"
                        + "<span class=\"post__like-heart\"[^>]*>♥</span>\\s*"
                        + "<span class=\"post__like-count\">1312</span>\\s*"
                        + "<span class=\"post__like-label\">いいね</span>\\s*</button>\\s*</form>\\s*"
                        + "<time class=\"post__created-at\".*")));
    }

    @Test
    @DisplayName("投稿詳細_存在しないid_404を返す")
    void 投稿詳細_存在しないid_404を返す() throws Exception {
        given(postService.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/posts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("投稿作成フォーム_GET_posts_new_空のフォームを表示する")
    void 投稿作成フォーム_GetPostsNew_空のフォームを表示する() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attribute("postForm", instanceOf(PostForm.class)));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("投稿作成フォーム_宮崎テーマの共通レイアウトを適用する")
    void 投稿作成フォーム_宮崎テーマの共通レイアウトを適用する() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<body class=\"miyazaki-shell\">")));
    }

    @Test
    @DisplayName("投稿作成フォーム_avatarColorを選択できる")
    void 投稿作成フォーム_avatarColorを選択できる() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<label for=\"avatarColor\">アバター色</label>\\s*"
                        + "<input[^>]+type=\"color\"[^>]+id=\"avatarColor\"[^>]+name=\"avatarColor\""
                        + "[^>]+value=\"#3498db\"[^>]*>.*")));
    }

    @Test
    @DisplayName("投稿作成フォーム_タグ入力フォームと候補リストを表示する")
    void 投稿作成フォーム_タグ入力フォームと候補リストを表示する() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<label for=\"tag-input\">タグ</label>\\s*"
                        + "<input[^>]+type=\"text\"[^>]+id=\"tag-input\"[^>]*>\\s*"
                        + "<div id=\"tag-suggestions\" class=\"tag-suggestions\""
                        + "\\s+data-suggestions-url=\"/tags/suggestions\" data-confirm-url=\"/tags\"></div>.*"
                        + "<script[^>]+src=\"/js/tag-suggestions.js\"[^>]+defer(?:=\"defer\")?[^>]*>"
                        + "</script>.*")));
    }

    @Test
    @DisplayName("投稿作成フォーム_確定タグ一覧領域を表示する")
    void 投稿作成フォーム_確定タグ一覧領域を表示する() throws Exception {
        mockMvc.perform(get("/posts/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<input[^>]+id=\"tag-input\"[^>]*>\\s*"
                        + "<div id=\"tag-suggestions\" class=\"tag-suggestions\""
                        + "\\s+data-suggestions-url=\"/tags/suggestions\" data-confirm-url=\"/tags\"></div>\\s*"
                        + "<div id=\"selected-tags\" class=\"tag-selected-tags\""
                        + " aria-label=\"確定済みタグ\"></div>\\s*"
                        + "<div id=\"tag-hidden-inputs\"></div>.*")));
    }

    @ParameterizedTest
    @CsvSource({
        "false, #3498db, #3498db",
        "true, #e91e63, #e91e63"
    })
    @DisplayName("投稿登録_avatarColor指定有無_投稿を作成して一覧へリダイレクトする")
    void 投稿登録_avatarColor指定有無_投稿を作成して一覧へリダイレクトする(
            boolean includeAvatarColor, String avatarColor, String expectedColor) throws Exception {
        MockHttpServletRequestBuilder request = post("/posts")
                .param("author", "alice")
                .param("body", "hello");
        if (includeAvatarColor) {
            request.param("avatarColor", avatarColor);
        }

        mockMvc.perform(request)
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts"));

        then(postService).should().create("alice", "hello", expectedColor, Collections.emptyList());
    }

    @Test
    @DisplayName("投稿登録_確定済みタグ名をServiceへ渡す")
    void 投稿登録_確定済みタグ名をServiceへ渡す() throws Exception {
        mockMvc.perform(post("/posts")
                        .param("author", "alice")
                        .param("body", "hello")
                        .param("avatarColor", "#3498db")
                        .param("tagNames", "spring", "研修"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts"));

        then(postService).should().create("alice", "hello", "#3498db", List.of("spring", "研修"));
    }

    @Test
    @DisplayName("投稿登録_postsスラッシュ_投稿を作成して一覧へリダイレクトする")
    void 投稿登録_postsスラッシュ_投稿を作成して一覧へリダイレクトする() throws Exception {
        mockMvc.perform(post("/posts/")
                        .param("author", "alice")
                        .param("body", "hello"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts"));

        then(postService).should().create("alice", "hello", "#3498db", Collections.emptyList());
    }

    @Test
    @DisplayName("投稿登録_author空白のみ_フォームを再表示し投稿者名必須エラーを表示する")
    void 投稿登録_author空白のみ_フォームを再表示し投稿者名必須エラーを表示する() throws Exception {
        mockMvc.perform(post("/posts")
                        .param("author", "   ")
                        .param("body", "hello"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "author"))
                .andExpect(content().string(containsString("投稿者名を入力してください")));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("投稿登録_author空文字_フォームを再表示し投稿者名必須エラーを表示する")
    void 投稿登録_author空文字_フォームを再表示し投稿者名必須エラーを表示する() throws Exception {
        mockMvc.perform(post("/posts")
                        .param("author", "")
                        .param("body", "hello"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "author"))
                .andExpect(content().string(containsString("投稿者名を入力してください")));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("投稿登録_author31文字_フォームを再表示し投稿者名文字数エラーを表示する")
    void 投稿登録_author31文字_フォームを再表示し投稿者名文字数エラーを表示する() throws Exception {
        mockMvc.perform(post("/posts")
                        .param("author", "a".repeat(31))
                        .param("body", "hello"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "author"))
                .andExpect(content().string(containsString("投稿者名は 30 文字以内で入力してください")));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("投稿登録_body空白のみ_フォームを再表示し本文必須エラーを表示する")
    void 投稿登録_body空白のみ_フォームを再表示し本文必須エラーを表示する() throws Exception {
        mockMvc.perform(post("/posts")
                        .param("author", "alice")
                        .param("body", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "body"))
                .andExpect(content().string(containsString("本文を入力してください")));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("投稿登録_body空文字_フォームを再表示し本文必須エラーを表示する")
    void 投稿登録_body空文字_フォームを再表示し本文必須エラーを表示する() throws Exception {
        mockMvc.perform(post("/posts")
                        .param("author", "alice")
                        .param("body", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "body"))
                .andExpect(content().string(containsString("本文を入力してください")));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("投稿登録_body281文字_フォームを再表示し本文文字数エラーを表示する")
    void 投稿登録_body281文字_フォームを再表示し本文文字数エラーを表示する() throws Exception {
        mockMvc.perform(post("/posts")
                        .param("author", "alice")
                        .param("body", "b".repeat(281)))
                .andExpect(status().isOk())
                .andExpect(view().name("posts/form"))
                .andExpect(model().attributeHasFieldErrors("postForm", "body"))
                .andExpect(content().string(containsString("本文は 280 文字以内で入力してください")));

        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("いいね_POST_posts_id_likes_clientHashでトグルし詳細へリダイレクトする")
    void いいね_PostPostsIdLikes_clientHashでトグルし詳細へリダイレクトする() throws Exception {
        mockMvc.perform(post("/posts/1/likes")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.1");
                            return request;
                        })
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts/1"));

        then(postService).should().toggleLike(1L, "c44b2068");
    }

    @Test
    @DisplayName("いいねトグル_同一クライアント2回押下_同じclientHashで2回トグルする")
    void いいねトグル_同一クライアント2回押下_同じclientHashで2回トグルする() throws Exception {
        mockMvc.perform(post("/posts/1/likes")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.1");
                            return request;
                        })
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts/1"));
        mockMvc.perform(post("/posts/1/likes")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.1");
                            return request;
                        })
                        .header("User-Agent", "Mozilla/5.0"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts/1"));

        then(postService).should(times(2)).toggleLike(1L, "c44b2068");
    }

    @Test
    @DisplayName("投稿削除_POST_posts_id_delete_Serviceで削除し一覧へリダイレクトする")
    void 投稿削除_PostPostsIdDelete_Serviceで削除し一覧へリダイレクトする() throws Exception {
        mockMvc.perform(post("/posts/1/delete"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/posts"));

        then(postService).should().deletePost(1L);
    }

    @Test
    @DisplayName("投稿削除_POST_posts_id_delete_存在しないid_404を返す")
    void 投稿削除_PostPostsIdDelete_存在しないid_404を返す() throws Exception {
        org.mockito.BDDMockito.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .given(postService).deletePost(999L);

        mockMvc.perform(post("/posts/999/delete"))
                .andExpect(status().isNotFound());
    }
}
