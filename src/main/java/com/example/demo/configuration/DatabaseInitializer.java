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

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            log.info("🗄️ Initializing database with default roles...");

            // Create ADMIN role if not exists
            if (roleRepository.findByName("ADMIN").isEmpty()) {
                Role adminRole = Role.builder()
                        .name("ADMIN")
                        .build();
                roleRepository.save(adminRole);
                log.info("✅ Created ADMIN role");
            } else {
                log.info("ℹ️ ADMIN role already exists");
            }

            // Create USER role if not exists
            if (roleRepository.findByName("USER").isEmpty()) {
                Role userRole = Role.builder()
                        .name("USER")
                        .build();
                roleRepository.save(userRole);
                log.info("✅ Created USER role");
            } else {
                log.info("ℹ️ USER role already exists");
            }

            log.info("🎯 Database initialization completed!");
        };
    }
}
