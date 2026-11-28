package com.bibliomesh.auth.internal;

import com.bibliomesh.auth.AdminUser;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final long TTL_SECONDS = 60 * 60 * 12; // 12 horas
    private final byte[] secretBytes;

    public JwtService(
        @Value("${app.jwt.secret:bibliomesh-dev-secret-change-in-prod-32bytes!}") String secret
    ) {
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(AdminUser user) {
        long now = Instant.now().getEpochSecond();
        long exp = now + TTL_SECONDS;

        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String schoolPart = user.getSchoolId() == null ? "null" : user.getSchoolId().toString();
        String payloadJson = String.format(
            "{\"sub\":\"%s\",\"role\":\"%s\",\"schoolId\":%s,\"iat\":%d,\"exp\":%d}",
            escape(user.getEmail()), escape(user.getRole()), schoolPart, now, exp
        );
        String payload = base64Url(payloadJson);
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public java.util.Optional<com.bibliomesh.auth.AuthPrincipal> verifyToken(String token) {
        if (token == null || token.isBlank()) return java.util.Optional.empty();
        String[] parts = token.split("\\.");
        if (parts.length != 3) return java.util.Optional.empty();

        String expectedSig = sign(parts[0] + "." + parts[1]);
        if (!java.security.MessageDigest.isEqual(
                expectedSig.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            return java.util.Optional.empty();
        }

        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String email = extractStringClaim(json, "sub");
            String role = extractStringClaim(json, "role");
            Long schoolId = extractLongClaim(json, "schoolId");
            Long exp = extractLongClaim(json, "exp");

            if (email == null || role == null || exp == null || Instant.now().getEpochSecond() >= exp) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new com.bibliomesh.auth.AuthPrincipal(email, role, schoolId));
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private String extractStringClaim(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\"" + key + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
            .matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"").replace("\\\\", "\\") : null;
    }

    private Long extractLongClaim(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("\"" + key + "\"\\s*:\\s*(null|-?\\d+)")
            .matcher(json);
        if (!m.find() || "null".equals(m.group(1))) return null;
        return Long.parseLong(m.group(1));
    }
}