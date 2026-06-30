package com.streetvendor.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TestJwt {
    public static void main(String[] args) {
        String secret = "e8a94b46c6a29e46a951c8a168a26d17e94b46c6a29e46a951c8a168a26d17e9"; // valid hex, but wait, decoder is base64!
        // valid base64 256-bit key:
        secret = "MEI5QzZFRUU0QTg0MkU5MkNGRTczN0Y1OTk4NEZCOEE2OUMxQTBEM0VFNEExNUE5OEMyOTQxQzRERUU4MzIxMQ==";
        SecretKey signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        long accessExpirationMs = 3600000;
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "CUSTOMER";

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);

        try {
            String token = Jwts.builder()
                    .claims(claims)
                    .subject(userId.toString())
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                    .signWith(signingKey)
                    .compact();
            System.out.println("Success: " + token);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
