package com.skillforge.authservice.security;

import com.skillforge.authservice.entity.UserRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerTokenWithRoleAuthority() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        when(jwtService.isTokenValid("signed-token")).thenReturn(true);
        when(jwtService.extractUserId("signed-token")).thenReturn(42L);
        when(jwtService.extractRole("signed-token"))
                .thenReturn(Optional.of(UserRole.ADMIN));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer signed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        new JwtAuthenticationFilter(jwtService)
                .doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(42L, authentication.getPrincipal());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority());
        verify(chain).doFilter(request, response);
    }
}
