package com.codequest.controller;

import com.codequest.config.AppProperties;
import com.codequest.dto.AuthDtos.*;
import com.codequest.security.UserPrincipal;
import com.codequest.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controllers are thin: read the request, call ONE service method, return a DTO.
 * @RestController = @Controller + @ResponseBody (return values are written as JSON).
 */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AppProperties props;

    public AuthController(AuthService auth, AppProperties props) {
        this.auth = auth;
        this.props = props;
    }

    @GetMapping("/config")
    public AuthConfig config() {
        return new AuthConfig(props.registrationEnabled());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // 201: a new resource (the account) was created
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204: done, nothing to return
    public void logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req);
    }

    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal UserPrincipal me) {
        return auth.me(me.id());
    }
}
