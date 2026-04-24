package com.pulse.desktop.service;

import com.pulse.desktop.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;

public class GameCoverStorageService {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String storeCover(Path sourceFile) throws IOException {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IOException("Fichier image introuvable.");
        }

        String ext = extensionLower(sourceFile.getFileName() == null ? "" : sourceFile.getFileName().toString());
        if (!isAllowedImageExtension(ext)) {
            throw new IOException("Format non supporte. Utilisez png/jpg/jpeg/webp/gif.");
        }

        Path uploadDirectory = AppConfig.webRootPath()
                .resolve("public")
                .resolve("uploads")
                .resolve("admin")
                .resolve("games");
        Files.createDirectories(uploadDirectory);

        String name = "game_cover_" + randomHex(12) + ext;
        Path destination = uploadDirectory.resolve(name).normalize();
        Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);

        // Store as web-relative path (without "public/") so it matches Symfony conventions and ImageResolver candidates.
        return "uploads/admin/games/" + name;
    }

    private static boolean isAllowedImageExtension(String ext) {
        return ".png".equals(ext) || ".jpg".equals(ext) || ".jpeg".equals(ext)
                || ".webp".equals(ext) || ".gif".equals(ext);
    }

    private static String extensionLower(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).trim().toLowerCase();
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[Math.max(4, bytes)];
        RANDOM.nextBytes(buf);
        StringBuilder out = new StringBuilder(buf.length * 2);
        for (byte b : buf) {
            out.append(Character.forDigit((b >>> 4) & 0xF, 16));
            out.append(Character.forDigit(b & 0xF, 16));
        }
        return out.toString();
    }
}

