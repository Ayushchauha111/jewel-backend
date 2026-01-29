package com.example.jewell.utils;

public class RazorpayUtils {

    public static String generateSignature(String data, String secret) throws Exception {
        javax.crypto.Mac sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secretKey);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes());
        return javax.xml.bind.DatatypeConverter.printHexBinary(hash).toLowerCase();
    }
}
