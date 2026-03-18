package com.parking.smartparking.controller;

import com.parking.smartparking.model.User;
import com.parking.smartparking.repository.UserRepository;
import com.parking.smartparking.service.AuthTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam("username") String username, @RequestParam("password") String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists!"));
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // Raw storage (Usually BCrypt hashes are used in prod!)
        userRepository.save(user);
        
        String token = tokenService.createTokenForUser(username);
        return ResponseEntity.ok(Map.of("message", "Registered successfully!", "token", token, "username", username));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            String token = tokenService.createTokenForUser(username);
            return ResponseEntity.ok(Map.of("message", "Login successful!", "token", token, "username", username));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials. Please try again."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenService.removeToken(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out securely."));
    }
}
