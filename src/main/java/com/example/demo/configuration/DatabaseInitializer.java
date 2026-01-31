package com.example.demo.configuration;

import com.example.demo.entity.Role;
import com.example.demo.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Database initialization configuration
 * Automatically creates default roles when application starts
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer {

    private final RoleRepository roleRepository;
    private final com.example.demo.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            log.info("🗄️ Initializing database with default roles and users...");

            // Create ADMIN role if not exists
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                Role role = Role.builder().name("ADMIN").build();
                log.info("✅ Created ADMIN role");
                return roleRepository.save(role);
            });

            // Create USER role if not exists
            Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
                Role role = Role.builder().name("USER").build();
                log.info("✅ Created USER role");
                return roleRepository.save(role);
            });

            // Create EXPERT role if not exists
            Role expertRole = roleRepository.findByName("EXPERT").orElseGet(() -> {
                Role role = Role.builder().name("EXPERT").build();
                log.info("✅ Created EXPERT role");
                return roleRepository.save(role);
            });

            // Create default Expert user (expert1 / password)
            if (userRepository.findByUsername("expert1").isEmpty()) {
                var expertUser = com.example.demo.entity.User.builder()
                        .username("expert1")
                        .password(passwordEncoder.encode("password"))
                        .fullName("Chuyên gia Tâm Lý")
                        .email("expert1@calmistry.com")
                        .roles(java.util.Set.of(expertRole))
                        .build();
                userRepository.save(expertUser);
                log.info("✅ Created default user: expert1");
            }

            // Create Admin VIP user (admin_vip / password)
            if (userRepository.findByUsername("admin_vip").isEmpty()) {
                var adminUser = com.example.demo.entity.User.builder()
                        .username("admin_vip")
                        .password(passwordEncoder.encode("password"))
                        .fullName("Super Administrator")
                        .email("admin_vip@calmistry.com")
                        .roles(java.util.Set.of(adminRole))
                        .build();
                userRepository.save(adminUser);
                log.info("✅ Created default admin: admin_vip");
            }

            log.info("🎯 Database initialization completed!");
        };
    }
}
