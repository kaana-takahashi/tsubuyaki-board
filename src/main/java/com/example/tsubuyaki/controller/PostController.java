package com.example.tsubuyaki.controller;

import com.example.tsubuyaki.domain.Post;
import com.example.tsubuyaki.domain.Tag;
import com.example.tsubuyaki.service.PostService;
import com.example.tsubuyaki.web.ClientHashGenerator;
import com.example.tsubuyaki.web.dto.PostForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class PostController {

    private final PostService postService;
    private final ClientHashGenerator clientHashGenerator = new ClientHashGenerator();

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping({ "/", "/posts", "/posts/" })
    public String list(@RequestParam(required = false) String q, Model model) {
        if (q == null) {
            model.addAttribute("posts", postService.latest());
        } else {
            model.addAttribute("posts", postService.list(q));
            model.addAttribute("q", q);
        }
        return "posts/list";
    }

    @GetMapping("/posts/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Post post = postService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("post", post);
        model.addAttribute("likeCount", postService.countLikes(id));
        return "posts/detail";
    }

    @GetMapping("/tags/suggestions")
    @ResponseBody
    public List<String> suggestTags(@RequestParam(defaultValue = "") String q) {
        return postService.suggestTagNames(q);
    }

    @PostMapping("/tags")
    @ResponseBody
    public TagResponse confirmTag(@RequestParam String name) {
        Tag tag = postService.confirmTag(name);
        return new TagResponse(tag.getName());
    }

    @GetMapping("/tags/{name}")
    public String listByTag(
            @PathVariable String name,
            @RequestParam(required = false) String sort,
            Model model) {
        String resolvedSort = resolveTagSort(sort);
        model.addAttribute("posts", postService.listByTag(name, resolvedSort));
        model.addAttribute("tagName", name);
        model.addAttribute("sort", resolvedSort);
        return "posts/list";
    }

    @GetMapping("/posts/new")
    public String newForm(Model model) {
        model.addAttribute("postForm", new PostForm());
        return "posts/form";
    }

    @PostMapping({ "/posts", "/posts/" })
    public String create(@Valid @ModelAttribute("postForm") PostForm postForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "posts/form";
        }
        postService.create(
                postForm.getAuthor(),
                postForm.getBody(),
                postForm.getAvatarColor(),
                postForm.getTagNames());
        return "redirect:/posts";
    }

    @PostMapping("/posts/{id}/likes")
    public String toggleLike(@PathVariable Long id, HttpServletRequest request) {
        String clientHash = clientHashGenerator.generate(request.getRemoteAddr(), request.getHeader("User-Agent"));
        postService.toggleLike(id, clientHash);
        return "redirect:/posts/" + id;
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id) {
        postService.deletePost(id);
        return "redirect:/posts";
    }

    public record TagResponse(String name) {
    }

    private String resolveTagSort(String sort) {
        if ("popular".equals(sort)) {
            return "popular";
        }
        return "latest";
    }
}
