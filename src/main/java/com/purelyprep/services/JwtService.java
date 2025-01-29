package com.purelyprep.services;

import com.purelyprep.pojo.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.expiration.ms}")
    private long jwtExpiration;

    @Value("${jwt.audience}")
    private String audience;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${environment.name}")
    public String environmentName;

    private final Key key;

    public JwtService(@Value("${jwt.secret}") String secretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("user", user.safeClone());
        extraClaims.put("environment", environmentName);
        return generateToken(extraClaims, user);
    }

    public String generateToken(Map<String, Object> extraClaims, User user) {
        return buildToken(extraClaims, user, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, User user, long expirationMs) {
        return Jwts
            .builder()
            .setClaims(extraClaims)
            .setSubject(user.username)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .setAudience(audience)
            .setIssuer(issuer)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean isTokenValid(String token) {
        Claims claims = extractAllClaims(token, key);
        return !isTokenExpired(claims) &&
                claims.get("environment").toString().equals(environmentName);
    }

    public User getUser(String token) {
        Claims claims = extractAllClaims(token, key);
        return (User) claims.get("user");
    }

    private Claims extractAllClaims(String token, Key key) {
        return Jwts
                .parserBuilder()
                .setSigningKey(key)
                .requireAudience(audience)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

}