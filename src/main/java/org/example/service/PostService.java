package org.example.service;

import org.example.entity.Post;
import org.example.repository.PostRepository;
import org.example.util.HtmlSanitizer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> all() {
        return postRepository.findAll();
    }

    public Post create(String title, String content) {
        // очищаем ввод от HTML/скриптов
        String safeTitle = HtmlSanitizer.sanitize(title);
        String safeContent = HtmlSanitizer.sanitize(content);
        return postRepository.save(new Post(safeTitle, safeContent));
    }
}