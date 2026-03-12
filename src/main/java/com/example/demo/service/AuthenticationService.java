package com.example.demo.service;

import com.example.demo.dto.request.AuthenticationRequest;
import com.example.demo.dto.request.GoogleLoginRequest;
import com.example.demo.dto.request.IntrospectRequest;
import com.example.demo.dto.request.UserRegistrationRequest;
import com.example.demo.dto.response.AuthenticationResponse;
import com.example.demo.dto.response.IntrospectResponse;
import com.example.demo.dto.response.UserRegistrationResponse;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.InvalidatedTokenRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    final UserRepository userRepository;
    final RoleRepository roleRepository;
    final InvalidatedTokenRepository invalidatedTokenRepository;
    final PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    @NonFinal
    @Value("${google.client-id}")
    protected String GOOGLE_CLIENT_ID;

    @NonFinal
    protected final String GRANT_TYPE = "authorization_code";

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    /**
     * Register a new user
     */
    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        log.info("👤 Registering new user with email: {}", request.getEmail());

        // Check if email already exists (primary login credential)
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        // Create new user
        User newUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .createdAt(LocalDateTime.now())
                .build();

        // Save user first
        User savedUser = userRepository.save(newUser);

        // Assign default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        savedUser.getRoles().add(userRole);

        // Save again to persist the role relationship
        savedUser = userRepository.save(savedUser);

        log.info("✅ User registered successfully with email: {}", savedUser.getEmail());

        return UserRegistrationResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .message("Registration successful! Please login with your email.")
                .build();
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("🔐 [Login] Attempting authentication for email: {}", request.getEmail());

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.error("❌ [Login] User not found: {}", request.getEmail());
                    return new AppException(ErrorCode.INVALID_CREDENTIALS);
                });

        log.info("✅ [Login] User found: {}, roles: {}", user.getUsername(), user.getRoles().size());

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("🔑 [Login] Password match: {}", authenticated);

        if (!authenticated) {
            log.error("❌ [Login] Password mismatch for user: {}", user.getUsername());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("🎫 [Login] Generating token for user: {}", user.getUsername());
        var token = generateToken(user);
        log.info("✅ [Login] Authentication successful for: {}", user.getUsername());

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .isOnboarded(user.getIsOnboarded() != null && user.getIsOnboarded())
                .build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                        .getJWTClaimsSet()
                        .getIssueTime()
                        .toInstant()
                        .plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                        .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!verified || expiryTime.before(new Date()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;

    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("mito.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }

    }

    public void logout(IntrospectRequest request) throws ParseException, JOSEException {
        if (request.getToken() == null) {
            log.warn("Logout attempt with null token");
            return;
        }
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            com.example.demo.entity.InvalidatedToken invalidatedToken = com.example.demo.entity.InvalidatedToken
                    .builder()
                    .id(jit)
                    .expiryTime(expiryTime)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token already expired or invalid");
        }
    }

    /**
     * Authenticate with Google ID Token
     */
    @Transactional
    public AuthenticationResponse authenticateWithGoogle(GoogleLoginRequest request) {
        log.info("🔐 [Google Login] Verifying Google ID token...");

        try {
            // Verify Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                log.error("❌ [Google Login] Invalid Google ID token");
                throw new AppException(ErrorCode.INVALID_CREDENTIALS);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();
            String fullName = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            log.info("✅ [Google Login] Token verified. Email: {}, Name: {}", email, fullName);

            // Find existing user by googleId or email
            User user = userRepository.findByEmail(email).orElse(null);

            if (user == null) {
                // Create new user from Google account
                log.info("👤 [Google Login] Creating new user for: {}", email);
                user = User.builder()
                        .username(email) // Use email as username
                        .email(email)
                        .fullName(fullName)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Google users don't use password login
                        .googleId(googleId)
                        .avatarUrl(pictureUrl)
                        .createdAt(LocalDateTime.now())
                        .build();

                User savedUser = userRepository.save(user);

                // Assign default USER role
                Role userRole = roleRepository.findByName("USER")
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
                savedUser.getRoles().add(userRole);
                user = userRepository.save(savedUser);

                log.info("✅ [Google Login] New user created with email: {}", email);
            } else {
                // Update Google info if needed
                if (user.getGoogleId() == null) {
                    user.setGoogleId(googleId);
                }
                if (pictureUrl != null) {
                    user.setAvatarUrl(pictureUrl);
                }
                user = userRepository.save(user);
                log.info("✅ [Google Login] Existing user found: {}", email);
            }

            // Generate JWT token
            var token = generateToken(user);
            log.info("🎫 [Google Login] JWT generated for: {}", user.getUsername());

            return AuthenticationResponse.builder()
                    .token(token)
                    .authenticated(true)
                    .isOnboarded(user.getIsOnboarded() != null && user.getIsOnboarded())
                    .build();

        } catch (GeneralSecurityException | IOException e) {
            log.error("❌ [Google Login] Error verifying token: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> {
                        stringJoiner.add(permission.getName());
                    });

            });
        return stringJoiner.toString();
    }

}
