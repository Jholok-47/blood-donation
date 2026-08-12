package com.lifelink.blood_donation.Config;

import com.lifelink.blood_donation.Entities.Enums.Role;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DevAdminSeeder {

    // TEMPORARY — for local testing only, until admin provisioning (open question) is decided.
    @Bean
    CommandLineRunner seedAdmin(UserRepository userRepository, PasswordEncoder encoder) {
        return args -> {
            if (userRepository.findByEmail("admin@bloodplatform.com").isEmpty()) {
                User admin = User.builder()
                        .fullName("System Admin")
                        .email("admin@bloodplatform.com")
                        .password(encoder.encode("admin123"))
                        .phone("0000000000")
                        .role(Role.ADMIN)
                        .verified(true)
                        .available(false)
                        .build();
                userRepository.save(admin);
            }
        };
    }
}
