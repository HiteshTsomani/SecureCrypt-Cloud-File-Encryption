package com.securecrypt.controller;

import com.securecrypt.model.User;
import com.securecrypt.repository.UserRepository;
import com.securecrypt.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller managing registration and sign-in operations.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Registers a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest authRequest) {
        if (userRepository.existsByUsername(authRequest.getUsername())) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Username is already taken.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Salt and hash the password using BCrypt
        String hashedPassword = passwordEncoder.encode(authRequest.getPassword());
        User user = new User(authRequest.getUsername(), hashedPassword);
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "User registered successfully!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Authenticates user and issues a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest authRequest) {
        return userRepository.findByUsername(authRequest.getUsername())
                .filter(user -> passwordEncoder.matches(authRequest.getPassword(), user.getPassword()))
                .map(user -> {
                    String token = tokenProvider.generateToken(user.getUsername());
                    Map<String, String> response = new HashMap<>();
                    response.put("token", token);
                    response.put("username", user.getUsername());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, String> response = new HashMap<>();
                    response.put("error", "Invalid username or password.");
                    return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
                });
    }

    // Static Request DTO
    public static class AuthRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
