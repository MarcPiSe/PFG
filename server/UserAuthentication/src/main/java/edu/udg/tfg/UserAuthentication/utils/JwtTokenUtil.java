package edu.udg.tfg.UserAuthentication.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
//import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import edu.udg.tfg.UserAuthentication.entities.UserEntity;
import edu.udg.tfg.UserAuthentication.services.UserService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil {

    @Value("classpath:private_key.pem")
    private Resource privateFile;

    private PrivateKey privateKey;

    @Autowired
    private UserService userService;

    private final long ACCESS_TOKEN_VALIDITY = 1000 * 60 * 60; // 1 hour
    private final long REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 7; // 7 days

    @PostConstruct
    public void init() throws Exception {
        privateKey = loadPrivateKey();
    }

    private PrivateKey loadPrivateKey() throws Exception {
        InputStream inputStream = privateFile.getInputStream();
        byte[] keyBytes = inputStream.readAllBytes();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractId(String token) {
        return extractClaim(token, Claims::getId);
    }

    public String extractConnectionId(String token) {
        return extractClaim(token, claims -> claims.get("connection-id", String.class));
    }

    public String extractPasswordChangedAt(String token) {
        return extractClaim(token, claims -> claims.get("password-changed-at", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(privateKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String reqUsername) {
        try {
            final String username = extractUsername(token);
            
            if (!username.equals(reqUsername) || isTokenExpired(token)) {
                return false;
            }
            
            return validatePasswordChangeTimestamp(token, username);
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateTokenExpiration(String token) {
        try {
            if (isTokenExpired(token)) {
                return false;
            }
            
            String username = extractUsername(token);
            return validatePasswordChangeTimestamp(token, username);
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean validatePasswordChangeTimestamp(String token, String username) {
        try {
            UserEntity user = userService.loadUserByUsername(username);
            String tokenPasswordTimestamp = extractPasswordChangedAt(token);
            
            if (tokenPasswordTimestamp == null || user.getLastPasswordChange() == null) {
                return false;
            }
            
            long tokenTime = Long.parseLong(tokenPasswordTimestamp);
            long userPasswordTime = user.getLastPasswordChange().getTime();
            
            return tokenTime >= userPasswordTime;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateToken(UserEntity user, boolean isRefreshToken) {
        long validity = isRefreshToken ? REFRESH_TOKEN_VALIDITY : ACCESS_TOKEN_VALIDITY;
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole());
        claims.put("connection-id", UUID.randomUUID().toString());
        claims.put("password-changed-at", String.valueOf(user.getLastPasswordChange().getTime()));
        return createToken(claims, user.getUsername(), validity);
    }

    public String generateToken(String username, boolean isRefreshToken) {
        UserEntity user = userService.loadUserByUsername(username);
        return generateToken(user, isRefreshToken);
    }

    private String createToken(Map<String, Object> claims, String username, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(privateKey).compact();
    }
}