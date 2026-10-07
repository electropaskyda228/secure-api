package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.entity.User;
import org.example.security.JwtService;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    /**
     * POST /auth/login — аутентификация пользователя.
     * Тело: {"username":"...", "password":"..."}
     * Ответ: {"token":"<JWT>", "type":"Bearer"}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> userOpt = userService.authenticate(
                request.getUsername(), request.getPassword());

        if (userOpt.isEmpty()) {
            // не раскрываем, что именно неверно (login или password)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
