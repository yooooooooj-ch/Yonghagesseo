package com.ddak.yongha.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JwtInviteUtil {
    private final Key key;
    private final String issuer;
    private final String audience;
    private final long ttlHours;

    public JwtInviteUtil(String secret, String issuer, String audience, long ttlHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
        this.ttlHours = ttlHours;
    }

    public String create(long parentNo, long childNo) {
        Instant now = Instant.now();
        return Jwts.builder()
            .claim("typ", "family-invite")
            .claim("p", parentNo)
            .claim("c", childNo)
            .setIssuer(issuer)
            .setAudience(audience)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(ttlHours * 3600)))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
            .requireIssuer(issuer)
            .requireAudience(audience)
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token);
    }
}
