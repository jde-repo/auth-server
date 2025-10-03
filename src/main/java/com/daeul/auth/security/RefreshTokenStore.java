package com.daeul.auth.security;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public void saveToken(String email, String refreshToken) {
        store.put(email, refreshToken);
    }

    public boolean validateToken(String email, String refreshToken) {
        return refreshToken.equals(store.get(email));
    }

    public void removeToken(String email) {
        store.remove(email);
    }
}
