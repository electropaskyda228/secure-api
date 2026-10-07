package org.example;

import org.example.entity.Post;
import org.example.repository.PostRepository;
import org.example.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SecureApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecureApiApplication.class, args);
    }

    // сидим демо-данные при старте (удобно для проверки)
    @Bean
    CommandLineRunner seed(UserService userService, PostRepository postRepository) {
        return args -> {
            if (userService.authenticate("alice", "password123").isEmpty()) {
                userService.register("alice", "password123", "USER");
                userService.register("admin", "admin12345", "ADMIN");
            }
            if (postRepository.count() == 0) {
                postRepository.save(new Post("Hello", "First post"));
                postRepository.save(new Post("Security", "Use JWT & bcrypt"));
            }
        };
    }
}
