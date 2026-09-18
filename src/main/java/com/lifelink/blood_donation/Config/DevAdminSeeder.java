package com.lifelink.blood_donation.Config;

import com.lifelink.blood_donation.Entities.Enums.AuthProvider;
import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DevAdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@lifelink.com").isPresent()) {
            return; // already seeded — nothing to do
        }

        User admin = User.builder()
                .fullName("Admin")
                .email("admin@lifelink.com")
                .password(passwordEncoder.encode("adminPassword"))
                .role(Role.ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .profileCompleted(true)
                .emailVerified(true)
                .build();

        userRepository.save(admin);
    }
}