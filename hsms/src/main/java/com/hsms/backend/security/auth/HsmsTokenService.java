package com.hsms.backend.security.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsms.backend.common.RoleCode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class HsmsTokenService {

    private static final String HMAC = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> JWT_MAP_TYPE = new JwtMapTypeReference();

    private final String secret;
    private final long ttlSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final class JwtMapTypeReference extends TypeReference<Map<String, Object>> {
    }

    public HsmsTokenService(
            @Value("${hsms.security.token-secret}") String secret,
            @Value("${hsms.security.token-ttl-seconds:28800}") long ttlSeconds
    ) {
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    @PostConstruct
    void validateConfiguration() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("HSMS_TOKEN_SECRET must contain at least 32 characters");
        }
    }

    public String issue(HsmsPrincipal principal) {
        long issuedAt = Instant.now().getEpochSecond();
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", principal.login());
        claims.put("uid", principal.id());
        claims.put("name", principal.displayName());
        claims.put("roles", principal.roles().stream().map(RoleCode::name).toList());
        claims.put("iat", issuedAt);
        claims.put("exp", expiresAt);
        String payload = encodeJson(claims);
        String signingInput = header + "." + payload;
        return signingInput + "." + sign(signingInput);
    }

    public HsmsPrincipal parse(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is empty");
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT structure is invalid");
        }
        String signingInput = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(signingInput), parts[2])) {
            throw new IllegalArgumentException("Token signature is invalid");
        }
        Map<String, Object> header = decodeJson(parts[0]);
        if (!"HS256".equals(header.get("alg"))) {
            throw new IllegalArgumentException("JWT algorithm is not supported");
        }
        Map<String, Object> claims = decodeJson(parts[1]);
        long expiresAt = number(claims.get("exp"));
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new IllegalArgumentException("Token expired");
        }
        Set<RoleCode> roles = new LinkedHashSet<>();
        Object roleClaims = claims.get("roles");
        if (roleClaims instanceof List<?> values) {
            values.stream().map(String::valueOf).map(RoleCode::valueOf).forEach(roles::add);
        } else if (roleClaims instanceof String values && !values.isBlank()) {
            Arrays.stream(values.split(",")).map(RoleCode::valueOf).forEach(roles::add);
        }
        return new HsmsPrincipal(number(claims.get("uid")), string(claims.get("sub")), string(claims.get("name")), roles);
    }

    private String encodeJson(Map<String, ?> value) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Cannot encode JWT", error);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(DECODER.decode(value), JWT_MAP_TYPE);
        } catch (IOException | IllegalArgumentException error) {
            throw new IllegalArgumentException("JWT JSON is invalid", error);
        }
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (InvalidKeyException | NoSuchAlgorithmException error) {
            throw new IllegalStateException("Cannot sign HSMS token", error);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }
}
