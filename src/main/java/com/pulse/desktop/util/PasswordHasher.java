package com.pulse.desktop.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {
    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        if (checkBcrypt(plainPassword, storedHash)) {
            return true;
        }

        String normalized = normalizeBcryptPrefix(storedHash);
        if (!normalized.equals(storedHash)) {
            return checkBcrypt(plainPassword, normalized);
        }
        return false;
    }

    private static boolean checkBcrypt(String plainPassword, String hash) {
        try {
            return BCrypt.checkpw(plainPassword, hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static String normalizeBcryptPrefix(String hash) {
        if (hash.startsWith("$2y$") || hash.startsWith("$2b$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }
}
