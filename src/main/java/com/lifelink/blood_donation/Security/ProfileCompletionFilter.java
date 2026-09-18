package com.lifelink.blood_donation.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class ProfileCompletionFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/complete-profile", "/logout", "/css", "/js", "/images", "/webjars"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = req.getRequestURI();

        boolean allowed = ALLOWED_PREFIXES.stream().anyMatch(path::startsWith);

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal
                && !allowed && !principal.getUser().isProfileCompleted()) {
            res.sendRedirect("/complete-profile");
            return;
        }
        chain.doFilter(req, res);
    }
}
