package org.example.service;

import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Создание пользователя с хэшированием пароля через BCrypt. */
    public User register(String username, String rawPassword, String role) {
        User u = new User(username, passwordEncoder.encode(rawPassword), role);
        return userRepository.save(u);
    }

    /** Проверка логина/пароля. Возвращает Optional с пользователем при успехе. */
    public Optional<User> authenticate(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()));
    }
}
