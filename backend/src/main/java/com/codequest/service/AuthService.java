package com.codequest.service;

import com.codequest.config.AppProperties;
import com.codequest.dto.AuthDtos.*;
import com.codequest.entity.Profile;
import com.codequest.entity.RefreshToken;
import com.codequest.entity.Role;
import com.codequest.entity.Streak;
import com.codequest.entity.User;
import com.codequest.exception.ApiException;
import com.codequest.mapper.Mappers;
import com.codequest.repository.ProfileRepository;
import com.codequest.repository.RefreshTokenRepository;
import com.codequest.repository.StreakRepository;
import com.codequest.repository.UserRepository;
import com.codequest.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuthService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final StreakRepository streaks;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppProperties props;
    private final Clock clock;

    public AuthService(UserRepository users, ProfileRepository profiles, StreakRepository streaks,
                       RefreshTokenRepository refreshTokens, PasswordEncoder encoder, JwtService jwt,
                       AppProperties props, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.streaks = streaks;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
        this.clock = clock;
    }

    /**
     * @Transactional: everything in this method is ONE database transaction. If saving the profile
     * fails, the user insert is rolled back too, so we never end up with half-created accounts.
     */
    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (!props.registrationEnabled()) throw ApiException.forbidden("Registration is closed on this server");
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists");

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDisplayName(req.displayName().trim());
        u.setRole(props.adminEmailList().contains(email) ? Role.ADMIN : Role.USER);
        users.save(u);

        Profile p = new Profile();
        p.setUserId(u.getId());
        p.setTimezone(props.defaultTimezone());
        p.setCharacterName(u.getDisplayName());
        profiles.save(p);

        Streak s = new Streak();
        s.setUserId(u.getId());
        streaks.save(s);

        return issueTokens(u);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .filter(User::isEnabled)
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad credentials")); // same message for both cases
        return issueTokens(u);
    }

    /** Rotation: the old refresh token is revoked and a new pair is issued. */
    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.findByTokenHash(JwtService.sha256(req.refreshToken()))
                .orElseThrow(() -> ApiException.unauthorized("Session expired, please log in again"));
        if (token.getRevokedAt() != null) {
            // A revoked token being reused can mean it was stolen: log out everywhere to be safe.
            refreshTokens.revokeAllForUser(token.getUserId(), now);
            throw ApiException.unauthorized("Session expired, please log in again");
        }
        if (!token.isActive(now)) throw ApiException.unauthorized("Session expired, please log in again");
        User u = users.findById(token.getUserId()).filter(User::isEnabled)
                .orElseThrow(() -> ApiException.unauthorized("Account disabled"));
        token.setRevokedAt(now);
        return issueTokens(u);
    }

    @Transactional
    public void logout(RefreshRequest req) {
        refreshTokens.findByTokenHash(JwtService.sha256(req.refreshToken()))
                .ifPresent(t -> t.setRevokedAt(clock.instant()));
    }

    @Transactional(readOnly = true)
    public UserView me(Long userId) {
        return users.findById(userId).map(Mappers::toView).orElseThrow(() -> ApiException.notFound("User"));
    }

    private TokenResponse issueTokens(User u) {
        String refresh = jwt.newRefreshTokenValue();
        RefreshToken rt = new RefreshToken();
        rt.setUserId(u.getId());
        rt.setTokenHash(JwtService.sha256(refresh));
        rt.setExpiresAt(jwt.refreshExpiry());
        refreshTokens.save(rt);
        refreshTokens.deleteExpired(clock.instant().minusSeconds(86400)); // light housekeeping, no timers needed
        return new TokenResponse(jwt.createAccessToken(u), refresh, jwt.accessTtlSeconds(), Mappers.toView(u));
    }
}
