package com.printflow.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class MetaWebhookSignature {
    private MetaWebhookSignature() {}
    public static boolean valid(byte[] body,String header,String secret){
        if(body==null||header==null||!header.matches("sha256=[0-9a-fA-F]{64}")||secret==null||secret.isBlank())return false;
        try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));byte[] expected=mac.doFinal(body);byte[] actual=HexFormat.of().parseHex(header.substring(7));return MessageDigest.isEqual(expected,actual);}catch(Exception ignored){return false;}
    }
}
