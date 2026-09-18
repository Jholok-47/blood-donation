package com.lifelink.blood_donation.Security;

import com.lifelink.blood_donation.Entities.Enums.AuthProvider;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);

        String googleId = oauth2User.getAttribute("sub");
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Google account has no email");
        }

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        if (user == null) {
            // Brand new user via Google — bypass OTP entirely, Google already verified the email.
            user = User.builder()
                    .fullName(name != null ? name : email)
                    .email(email)
                    .password(null)
                    .googleId(googleId)
                    .authProvider(AuthProvider.GOOGLE)
                    .emailVerified(true)
                    .profileCompleted(false)
                    .role(null)
                    .build();
            user = userRepository.save(user);
        } else if (user.getGoogleId() == null) {
            // ACCOUNT LINKING: an existing local account matched this Google email — link, don't duplicate.
            user.setGoogleId(googleId);
            user.setEmailVerified(true); // Google's verification supersedes any pending local OTP step
            user = userRepository.save(user);
        }

        return new UserPrincipal(user, oauth2User.getAttributes());
    }
}
