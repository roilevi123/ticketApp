package Infastructure;



import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TokenService {

    private final String SECRET = "my-secret-key-my-secret-key-my-secret-key";
    private final long EXPIRATION = 1000 * 60 * 60 * 24; // יום
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String generateToken(String username) {
        if (blacklist.contains(username)) {
            blacklist.remove(username);
        }
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        if (blacklist.contains(token)) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public void removeFromBlacklist(String token) {
        blacklist.remove(token);
    }
    public void addBlacklistToken(String token) {
        blacklist.add(token);
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    public void clearAllData() {
        blacklist.clear();
    }
}