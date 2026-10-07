package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.CreatePostRequest;
import org.example.entity.Post;
import org.example.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DataController {

    private final PostService postService;

    public DataController(PostService postService) {
        this.postService = postService;
    }

    /** GET /api/data — защищённый эндпоинт (нужен JWT). */
    @GetMapping("/data")
    public List<Post> getData() {
        return postService.all();
    }

    /** POST /api/posts — защищённый эндпоинт (третий, придуманный самостоятельно). */
    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(@Valid @RequestBody CreatePostRequest req) {
        Post p = postService.create(req.getTitle(), req.getContent());
        return ResponseEntity.ok(p);
    }
}
