package com.jrappspot.cashlipi.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LockUtils {

    /** SHA-256 hash of a string */
    public static String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return input; // fallback (shouldn't happen)
        }
    }

    public static boolean verify(String input, String storedHash) {
        return hash(input).equals(storedHash);
    }

    /** Convert pattern node list like [0,1,2,5,8] → string for hashing */
    public static String patternToString(java.util.List<Integer> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Integer n : nodes) sb.append(n);
        return sb.toString();
    }
}
