package com.example.demo.configuration;

import com.example.demo.entity.Role;
import com.example.demo.entity.UserPlan;
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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            log.info("🗄️ Initializing database with default roles and users...");

            // Fix workshop_bookings status column if needed
            try {
                jdbcTemplate.execute("ALTER TABLE workshop_bookings MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'");
                log.info("Schema updated: workshop_bookings.status column is now VARCHAR(20).");
            } catch (Exception e) {
                log.warn("Could not alter workshop_bookings.status column (may already be correct): {}", e.getMessage());
            }

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
                        .plan(UserPlan.GOLD)
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
                        .plan(UserPlan.GOLD)
                        .build();
                userRepository.save(adminUser);
                log.info("✅ Created default admin: admin_vip");
            }

            // Migrate existing users:
            // - existing accounts (pre-plan column) => GOLD
            // - ADMIN/EXPERT => always GOLD
            try {
                var users = userRepository.findAll();
                boolean changed = false;
                for (var u : users) {
                    boolean isAdminOrExpert = u.getRoles() != null && u.getRoles().stream()
                            .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "EXPERT".equalsIgnoreCase(r.getName()));

                    if (isAdminOrExpert) {
                        if (u.getPlan() != UserPlan.GOLD) {
                            u.setPlan(UserPlan.GOLD);
                            changed = true;
                        }
                        continue;
                    }

                    if (u.getPlan() == null) {
                        u.setPlan(UserPlan.GOLD);
                        changed = true;
                    }
                }

                if (changed) {
                    userRepository.saveAll(users);
                    log.info("Schema updated: user plans migrated.");
                }
            } catch (Exception e) {
                log.warn("Could not migrate user plans (may already be done): {}", e.getMessage());
            }

            // Seed/fake workshop stats for demo UI (admin list): show 26/30 and COMPLETED for Matcha Date.
            try {
                int updated = jdbcTemplate.update(
                        "UPDATE workshops " +
                                "SET max_participants = 30, current_participants = 26, status = 'COMPLETED', " +
                                "start_time = COALESCE(start_time, '2026-03-12 09:00:00'), " +
                                "end_time = COALESCE(end_time, '2026-03-12 10:30:00') " +
                                "WHERE title LIKE ?",
                        "%Matcha Date%"
                );
                if (updated > 0) {
                    log.info("✅ Seeded Matcha Date workshop: 26/30, COMPLETED (rows updated: {}).", updated);
                }
            } catch (Exception e) {
                log.warn("Could not seed Matcha Date workshop demo data: {}", e.getMessage());
            }

            log.info("🎯 Database initialization completed!");
        };
    }
}
