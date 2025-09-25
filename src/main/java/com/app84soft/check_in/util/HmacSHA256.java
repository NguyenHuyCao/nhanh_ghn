package com.app84soft.check_in.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public final class HmacSHA256 {
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public static String encrypt(String message, String secretKey) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(), HMAC_SHA256_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(message.getBytes());
            return new String(Base64.getEncoder().encode(rawHmac));

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate HMAC", e);
        }
    }

}
