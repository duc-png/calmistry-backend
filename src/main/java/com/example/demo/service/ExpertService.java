package com.example.demo.service;

import com.example.demo.dto.request.ExpertRegisterRequest;
import com.example.demo.dto.response.ExpertRegisterResponse;
import com.example.demo.entity.ExpertProfile;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.ExpertProfileRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpertService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    ExpertProfileRepository expertProfileRepository;
    PasswordEncoder passwordEncoder;

    @Transactional
    public ExpertRegisterResponse registerExpert(ExpertRegisterRequest request) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // Tìm hoặc tạo role EXPERT
        Role expertRole = roleRepository.findByName("EXPERT")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName("EXPERT");
                    return roleRepository.save(newRole);
                });

        // Tạo user mới
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setIsActive(true);

        // Gán role EXPERT cho user
        Set<Role> roles = new HashSet<>();
        roles.add(expertRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        // Tạo ExpertProfile
        ExpertProfile expertProfile = new ExpertProfile();
        expertProfile.setUser(savedUser);
        expertProfile.setSpecialty(request.getSpecialty());
        expertProfile.setDegree(request.getDegree());
        expertProfile.setExperienceYears(request.getExperienceYears());
        expertProfile.setBio(request.getBio());
        expertProfile.setAvatarUrl(request.getAvatarUrl());
        expertProfile.setIsVerified(false); // Mặc định chưa verify, admin sẽ verify sau

        ExpertProfile savedExpertProfile = expertProfileRepository.save(expertProfile);

        log.info("Expert registered successfully: {}", savedUser.getUsername());

        return ExpertRegisterResponse.builder()
                .userId(savedUser.getId())
                .expertProfileId(savedExpertProfile.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .specialty(savedExpertProfile.getSpecialty())
                .message("Expert account created successfully")
                .build();
    }
}

