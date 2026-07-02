package com.example.tsubuyaki.service;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.PostLike;
import com.example.tsubuyaki.domain.PostTag;
import com.example.tsubuyaki.domain.Tag;
import com.example.tsubuyaki.repository.PostLikeRepository;
import com.example.tsubuyaki.repository.PostRepository;
import com.example.tsubuyaki.repository.PostTagRepository;
import com.example.tsubuyaki.repository.TagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class PostService {

    private static final Pattern AVATAR_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_-]{1,50}$");

    private final PostRepository repository;
    private final PostLikeRepository postLikeRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;

    public PostService(
            PostRepository repository,
            PostLikeRepository postLikeRepository,
            TagRepository tagRepository,
            PostTagRepository postTagRepository) {
        this.repository = repository;
        this.postLikeRepository = postLikeRepository;
        this.tagRepository = tagRepository;
        this.postTagRepository = postTagRepository;
    }

    public List<Post> latest() {
        return repository.findTop50ByDeletedAtIsNullOrderByCreatedAtDesc();
    }

    public List<Post> list(String q) {
        if (q == null || q.trim().isEmpty()) {
            return latest();
        }
        return repository.findTop50ByBodyContainingAndDeletedAtIsNullOrderByCreatedAtDesc(q);
    }

    public Optional<Post> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Post create(String author, String body) {
        return create(author, body, Post.DEFAULT_AVATAR_COLOR);
    }

    @Transactional
    public Post create(String author, String body, String avatarColor) {
        return create(author, body, avatarColor, Collections.emptyList());
    }

    @Transactional
    public Post create(String author, String body, String avatarColor, List<String> tagNames) {
        Post post = repository.save(new Post(author, body, normalizeAvatarColor(avatarColor), Instant.now()));
        normalizeTagNames(tagNames).forEach(tagName -> savePostTag(post, tagName));
        return post;
    }

    @Transactional
    public Tag confirmTag(String name) {
        String normalized = normalizeTagName(name);
        return tagRepository.findByName(normalized)
                .orElseGet(() -> tagRepository.save(new Tag(normalized)));
    }

    public List<Post> listByTag(String tagName) {
        return listByTag(tagName, "latest");
    }

    public List<Post> listByTag(String tagName, String sort) {
        if ("popular".equals(sort)) {
            return postTagRepository.findByTagNameOrderByLikeCountDescCreatedAtDesc(tagName, PageRequest.of(0, 50))
                    .stream()
                    .map(PostTag::getPost)
                    .toList();
        }
        return postTagRepository.findTop50ByTagNameOrderByPostCreatedAtDesc(tagName, PageRequest.of(0, 50))
                .stream()
                .map(PostTag::getPost)
                .toList();
    }

    public List<String> suggestTagNames(String q) {
        List<Tag> tags;
        if (q == null || q.trim().isEmpty()) {
            tags = tagRepository.findTop10ByOrderByNameAsc();
        } else {
            tags = tagRepository.findTop10ByNameStartingWithOrderByNameAsc(q.trim());
        }
        return tags.stream()
                .map(Tag::getName)
                .toList();
    }

    @Transactional
    public long toggleLike(Long postId, String clientHash) {
        Post post = repository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        postLikeRepository.findByPostIdAndClientHash(postId, clientHash)
                .ifPresentOrElse(postLikeRepository::delete,
                        () -> postLikeRepository.save(new PostLike(post, clientHash)));

        return postLikeRepository.countByPostId(postId);
    }

    public long countLikes(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = repository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        post.delete(Instant.now());
    }

    private String normalizeAvatarColor(String avatarColor) {
        if (avatarColor == null || avatarColor.trim().isEmpty()) {
            return Post.DEFAULT_AVATAR_COLOR;
        }
        String trimmed = avatarColor.trim();
        if (!AVATAR_COLOR_PATTERN.matcher(trimmed).matches()) {
            return Post.DEFAULT_AVATAR_COLOR;
        }
        return trimmed;
    }

    private Set<String> normalizeTagNames(List<String> tagNames) {
        Set<String> normalizedTagNames = new LinkedHashSet<>();
        if (tagNames == null) {
            return normalizedTagNames;
        }
        for (String tagName : tagNames) {
            normalizedTagNames.add(normalizeTagName(tagName));
        }
        return normalizedTagNames;
    }

    private void savePostTag(Post post, String tagName) {
        tagRepository.findByName(tagName)
                .ifPresent(tag -> postTagRepository.save(new PostTag(post, tag)));
    }

    private String normalizeTagName(String name) {
        if (name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String normalized = name.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1).trim();
        }
        if (!TAG_NAME_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }
}
