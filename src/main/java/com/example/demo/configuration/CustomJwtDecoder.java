package com.example.demo.configuration;

import com.example.demo.repository.InvalidatedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {
    @Value("${jwt.signerKey}")
    private String signerKey;

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        // Kiểm tra token có bị invalidate không
        try {
            log.info("🔑 [JWT] Decoding token... (length: {})", token != null ? token.length() : 0);

            // Decode token để lấy JTI
            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec)
                        .macAlgorithm(MacAlgorithm.HS512)
                        .build();
                log.info("🔑 [JWT] NimbusJwtDecoder initialized");
            }

            Jwt jwt = nimbusJwtDecoder.decode(token);
            log.info("🔑 [JWT] Token decoded successfully. Subject: {}, JTI: {}", jwt.getSubject(), jwt.getId());

            // Kiểm tra token có trong blacklist không
            String jti = jwt.getId();
            if (jti != null && invalidatedTokenRepository.existsById(jti)) {
                log.warn("🔑 [JWT] Token is blacklisted: {}", jti);
                throw new JwtException("Token has been invalidated");
            }

            log.info("✅ [JWT] Token validation successful");
            return jwt;
        } catch (JwtException e) {
            log.error("❌ [JWT] JwtException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ [JWT] Unexpected error: {}", e.getMessage(), e);
            throw new JwtException("Token invalid: " + e.getMessage());
        }
    }
}
