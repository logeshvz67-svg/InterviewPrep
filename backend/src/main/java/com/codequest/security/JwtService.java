package com.codequest.security;

import com.codequest.config.AppProperties;
import com.codequest.entity.Role;
import com.codequest.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Creates and checks tokens.
 *  - Access token: a signed JWT, valid 15 min. The server doesn't store it; the signature proves it's genuine.
 *  - Refresh token: a long random string, valid 14 days. Stored in the DB as a SHA-256 hash so it can be
 *    revoked (logout) and rotated (each refresh issues a new one and kills the old one).
 */
@Service
public class JwtService {
    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    // Constructor injection: Spring sees this constructor and passes in the AppProperties and Clock beans.
    // WHY constructor injection (not @Autowired on fields): fields can be final, and tests can just call `new`.
    public JwtService(AppProperties props, Clock clock) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTokenMinutes());
        this.refreshTtl = Duration.ofDays(props.jwt().refreshTokenDays());
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    /** Returns the caller if the token is valid and not expired, otherwise empty. */
    public Optional<UserPrincipal> parseAccessToken(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new UserPrincipal(Long.valueOf(c.getSubject()), c.get("email", String.class),
                    Role.valueOf(c.get("role", String.class))));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String newRefreshTokenValue() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Instant refreshExpiry() { return clock.instant().plus(refreshTtl); }

    public long accessTtlSeconds() { return accessTtl.toSeconds(); }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
