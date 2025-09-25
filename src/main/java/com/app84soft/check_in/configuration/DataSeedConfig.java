package com.app84soft.check_in.configuration;

import com.app84soft.check_in.dto.constant.ActiveStatus;
import com.app84soft.check_in.dto.constant.RoleType;
import com.app84soft.check_in.entities.user.User;
import com.app84soft.check_in.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeedConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedAdmin() {
        return args -> {
            String adminEmail = "admin@gmail.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User u = new User();
                u.setName("Super Admin");
                u.setEmail(adminEmail);
                u.setPassword(passwordEncoder.encode("123456"));
                u.setRoleType(RoleType.ADMIN);
                u.setStatus(ActiveStatus.ACTIVE);
                userRepository.save(u);
                System.out.println("Seeded admin: " + adminEmail);
            }
        };
    }
}

