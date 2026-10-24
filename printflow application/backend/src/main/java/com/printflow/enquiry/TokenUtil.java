package com.printflow.enquiry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class TokenUtil {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TokenUtil() {}
    public static String newToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public static String sha256(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    public static boolean matches(String input, String digest) {
        if (input == null || input.length() > 256 || digest == null) return false;
        return MessageDigest.isEqual(sha256(input).getBytes(StandardCharsets.UTF_8),
            digest.getBytes(StandardCharsets.UTF_8));
    }
}
