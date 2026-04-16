package com.pulse.desktop.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TwoFactorTotpService {
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int DIGITS = 6;
    private static final int PERIOD = 30;

    private final SecureRandom random = new SecureRandom();

    public String generateSecret(int length) {
        int safeLength = Math.max(16, Math.min(64, length));
        StringBuilder out = new StringBuilder(safeLength);
        for (int i = 0; i < safeLength; i++) {
            int index = random.nextInt(BASE32_ALPHABET.length());
            out.append(BASE32_ALPHABET.charAt(index));
        }
        return out.toString();
    }

    public String buildOtpAuthUri(String issuer, String accountLabel, String secret) {
        String safeIssuer = issuer == null || issuer.isBlank() ? "PULSE" : issuer.trim();
        String safeAccount = accountLabel == null || accountLabel.isBlank() ? "user" : accountLabel.trim();
        String encodedLabel = urlEncode(safeIssuer + ":" + safeAccount);

        return "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d".formatted(
                encodedLabel,
                urlEncode(secret == null ? "" : secret.trim()),
                urlEncode(safeIssuer),
                DIGITS,
                PERIOD
        );
    }

    public String buildQrCodeUrl(String otpAuthUri, int size) {
        int safeSize = Math.max(128, Math.min(512, size));
        return "https://api.qrserver.com/v1/create-qr-code/?size=%dx%d&data=%s".formatted(
                safeSize,
                safeSize,
                urlEncode(otpAuthUri == null ? "" : otpAuthUri)
        );
    }

    public boolean verifyCode(String secret, String code, int window) {
        String normalizedCode = (code == null ? "" : code).replaceAll("\\D+", "");
        if (normalizedCode.length() != DIGITS) {
            return false;
        }

        int safeWindow = Math.max(0, Math.min(3, window));
        long now = Instant.now().getEpochSecond();
        for (int offset = -safeWindow; offset <= safeWindow; offset++) {
            String candidate = generateCodeForTimestamp(secret, now + (long) offset * PERIOD);
            if (candidate.equals(normalizedCode)) {
                return true;
            }
        }
        return false;
    }

    private String generateCodeForTimestamp(String secret, long timestamp) {
        long counter = Math.max(0, timestamp) / PERIOD;
        byte[] key = decodeBase32(secret);
        if (key.length == 0) {
            return "000000";
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hmac = mac.doFinal(counterBytes(counter));

            int offset = hmac[hmac.length - 1] & 0x0f;
            int binaryCode = ((hmac[offset] & 0x7f) << 24)
                    | ((hmac[offset + 1] & 0xff) << 16)
                    | ((hmac[offset + 2] & 0xff) << 8)
                    | (hmac[offset + 3] & 0xff);

            int otp = binaryCode % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%06d", otp);
        } catch (Exception ex) {
            return "000000";
        }
    }

    private static byte[] counterBytes(long counter) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(counter);
        return buffer.array();
    }

    private static byte[] decodeBase32(String value) {
        if (value == null || value.isBlank()) {
            return new byte[0];
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z2-7]", "");
        if (normalized.isEmpty()) {
            return new byte[0];
        }

        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[(normalized.length() * 5) / 8];
        int outIndex = 0;

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int index = BASE32_ALPHABET.indexOf(c);
            if (index < 0) {
                continue;
            }
            buffer = (buffer << 5) | index;
            bitsLeft += 5;

            while (bitsLeft >= 8) {
                bitsLeft -= 8;
                if (outIndex < out.length) {
                    out[outIndex++] = (byte) ((buffer >> bitsLeft) & 0xff);
                }
            }
        }

        if (outIndex == out.length) {
            return out;
        }
        byte[] resized = new byte[outIndex];
        System.arraycopy(out, 0, resized, 0, outIndex);
        return resized;
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
