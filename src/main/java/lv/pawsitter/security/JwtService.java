package lv.pawsitter.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lv.pawsitter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final SecretKey secretSigningKey;

    private final UserRepository repository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        repository.findByEmail(userDetails.getUsername()).ifPresent(user -> {
            claims.put("userId", user.getId());
            claims.put("login", user.getEmail());
            claims.put("role", user.getRole().name());
        });
        return generateToken(claims, userDetails);
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .subject(userDetails.getUsername())
                .add(extraClaims)
                .and()
                .signWith(secretSigningKey)
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUserName(token);
        return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretSigningKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}