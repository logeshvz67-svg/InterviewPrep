package com.codequest.security;

import com.codequest.entity.Role;

/**
 * Who is calling, taken from a verified JWT. Controllers receive it with
 * @AuthenticationPrincipal UserPrincipal me, then pass me.id() to services.
 */
public record UserPrincipal(Long id, String email, Role role) {}
