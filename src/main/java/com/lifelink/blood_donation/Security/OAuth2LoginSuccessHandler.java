package com.lifelink.blood_donation.Security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        if (!principal.getUser().isProfileCompleted()) {
            response.sendRedirect("/complete-profile");
            return;
        }
        response.sendRedirect(switch (principal.getUser().getRole()) {
            case PATIENT -> "/patient/dashboard";
            case DONOR -> "/donor/dashboard";
            case ADMIN -> "/admin/dashboard";
        });
    }
}
