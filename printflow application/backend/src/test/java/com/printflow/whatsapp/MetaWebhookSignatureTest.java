package com.printflow.whatsapp;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetaWebhookSignatureTest {
    private static final String SECRET="synthetic-meta-app-secret";
    private static final byte[] BODY="{\"entry\":[]}".getBytes(StandardCharsets.UTF_8);
    private static String signature(byte[] body)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return "sha256="+HexFormat.of().formatHex(mac.doFinal(body));}
    @Test void acceptsValidConstantTimeSignatureAndRejectsTampering(){assertTrue(MetaWebhookSignature.valid(BODY,assertDoesNotThrow(()->signature(BODY)),SECRET));assertFalse(MetaWebhookSignature.valid(BODY,"sha256="+"0".repeat(64),SECRET));assertFalse(MetaWebhookSignature.valid(BODY,null,SECRET));assertFalse(MetaWebhookSignature.valid("tampered".getBytes(StandardCharsets.UTF_8),assertDoesNotThrow(()->signature(BODY)),SECRET));assertFalse(MetaWebhookSignature.valid(BODY,assertDoesNotThrow(()->signature(BODY)),""));}
}
