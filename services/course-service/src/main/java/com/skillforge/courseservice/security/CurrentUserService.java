package com.skillforge.courseservice.security;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthenticatedUser getCurrentUser(
            JwtAuthenticationToken authentication
    ) {
        var token = authentication.getToken();
        return new AuthenticatedUser(
                Long.valueOf(token.getSubject()),
                token.getClaimAsString("email"),
                token.getClaimAsString("role")
        );
    }
}
