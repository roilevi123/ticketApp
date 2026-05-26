package com.ticketing.ticketapp.Infastructure;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class TokenService {

    private final String SECRET = "my-secret-key-my-secret-key-my-secret-key";
    private final long EXPIRATION = 1000 * 60 * 60 * 24;
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();
    private final Set<String> bannedUserIds = ConcurrentHashMap.newKeySet();

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String generateMemberToken(String userId, String username) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", "MEMBER")
                .claim("username", username)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateGuestToken() {
        String guestId = UUID.randomUUID().toString();
        return Jwts.builder()
                .setSubject(guestId)
                .claim("role", "GUEST")
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
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return !bannedUserIds.contains(claims.getSubject());
        } catch (Exception e) {
            return false;
        }
    }

    public void addBlacklistToken(String token) {
        blacklist.add(token);
    }

    public void banUser(String userId) {
        bannedUserIds.add(userId);
    }

    public void unbanUser(String userId) {
        bannedUserIds.remove(userId);
    }

//    public boolean isUserBanned(String userId) {
//        return bannedUserIds.contains(userId);
//    }
//
//    public void cleanExpiredBlacklistTokens() {
//        Iterator<String> iterator = blacklist.iterator();
//        while (iterator.hasNext()) {
//            String token = iterator.next();
//            try {
//                Jwts.parserBuilder()
//                        .setSigningKey(key)
//                        .build()
//                        .parseClaimsJws(token);
//            } catch (ExpiredJwtException e) {
//                iterator.remove();
//            } catch (Exception e) {
//            }
//        }
//    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

//    public String extractRole(String token) {
//        return extractClaim(token, claims -> claims.get("role", String.class));
//    }
//
//    public Date extractExpiration(String token) {
//        return extractClaim(token, Claims::getExpiration);
//    }

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
        bannedUserIds.clear();
    }
}
