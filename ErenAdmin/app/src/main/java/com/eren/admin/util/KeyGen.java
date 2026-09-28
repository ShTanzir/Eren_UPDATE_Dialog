package com.eren.admin.util;

import java.util.Random;

public final class KeyGen {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RND = new Random();

    /** Format: MM-XXX-XXX-XXX-ST */
    public static String generateConnectKey() {
        return "MM-" + block() + "-" + block() + "-" + block() + "-ST";
    }

    public static String generateAccessKey() {
        return "EK-" + block() + "-" + block() + "-" + block();
    }

    private static String block() {
        StringBuilder sb = new StringBuilder(3);
        for (int i = 0; i < 3; i++) {
            sb.append(CHARS.charAt(RND.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
