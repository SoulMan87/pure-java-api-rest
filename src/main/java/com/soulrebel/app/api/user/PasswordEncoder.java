package com.soulrebel.app.api.user;

import io.vavr.control.Try;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class PasswordEncoder {

    public static String encodePassword(String password) {
        return Try.of (() -> {
            MessageDigest digest = MessageDigest.getInstance ("SHA-256");

            byte[] passwordBytes = password.getBytes (StandardCharsets.UTF_8);

            byte[] hashedBytes = digest.digest (passwordBytes);

            return bytesToHex (hashedBytes);
        }).getOrElseThrow (() -> new RuntimeException ("SHA-256 algorithm not available"));
    }

    private static String bytesToHex(byte[] bytes) {

        StringBuilder hexString = new StringBuilder ();
        for (byte hashedByte : bytes) {
            String hex = Integer.toHexString (0xff & hashedByte);
            if (hex.length () == 1) {
                hexString.append ('0');
            }
            hexString.append (hex);
        }
        return hexString.toString ();
    }

}
