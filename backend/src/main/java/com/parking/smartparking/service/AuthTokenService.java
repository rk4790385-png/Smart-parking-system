package com.parking.smartparking.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Service
public class AuthTokenService {
    // In-memory token storage for hyper-fast Insane Level authentication.
    private final Map<String, String> sessionTokens = new HashMap<>();

    public String createTokenForUser(String username) {
        String token = UUID.randomUUID().toString();
        sessionTokens.put(token, username);
        return token;
    }

    public boolean isTokenValid(String token) {
        return sessionTokens.containsKey(token);
    }

    public void removeToken(String token) {
        sessionTokens.remove(token);
    }
}
