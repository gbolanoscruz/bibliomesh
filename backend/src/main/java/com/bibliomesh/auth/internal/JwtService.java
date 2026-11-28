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
}