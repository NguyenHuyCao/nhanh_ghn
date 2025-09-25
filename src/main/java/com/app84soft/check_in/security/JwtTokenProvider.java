package com.app84soft.check_in.security;


import com.app84soft.check_in.util.Util;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Log4j2
public class JwtTokenProvider {
    @Value("${app.jwtAdminExpirationInMs}")
    private long expirationInMs;
    @Value("${app.jwtSecretAdmin}")
    private String jwtSecret;

    public String generateToken(final String subId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMs);
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(subId)
                .issuedAt(new Date())
                .expiration(expiryDate)
                .signWith(key, Jwts.SIG.HS512).compact();
    }

    public String getSubIdFromJwt(final String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        return claims.getSubject();
    }

    public boolean validateToken(final String authToken) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String sub = Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken).getPayload().getSubject();
            if (StringUtils.isBlank(sub)) {
                throw new Exception(authToken);
            }
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token. " + ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token. " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token. " + ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT claims string is empty. " + ex.getMessage());
        }
        return false;
    }

    public Map<String, Object> getPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.out.println("Invalid JWT token format.");
                return new HashMap<>();
            }
            String payloadBase64 = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payloadBase64);
            String decodedPayload = new String(decodedBytes);
            return Util.stringToObject(Map.class, decodedPayload);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

}
