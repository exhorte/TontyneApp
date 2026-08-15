package sn.isi.tontyn.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Le jeton porte desormais l'identifiant numerique, l'e-mail (sub) et le role. */
    /** Le sujet du jeton porte l'identifiant du compte : son numero de telephone. */
    public String generateToken(Long id, String identifiant, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(identifiant)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key())
                .compact();
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }

    public String extractIdentifiant(String token) {
        return claims(token).getSubject();
    }

    public Long extractId(String token) {
        return claims(token).get("id", Long.class);
    }

    public boolean isValid(String token) {
        try {
            claims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
