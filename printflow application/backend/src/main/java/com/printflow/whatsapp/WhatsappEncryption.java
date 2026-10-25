package com.printflow.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WhatsappEncryption {
    private final String configured;
    public WhatsappEncryption(@Value("${APP_WHATSAPP_ENCRYPTION_KEY:}") String configured){this.configured=configured;}
    public boolean configured(){try{return Base64.getDecoder().decode(configured).length==32;}catch(IllegalArgumentException e){return false;}}
    private SecretKeySpec key(){if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"WhatsApp encryption key is not configured");return new SecretKeySpec(Base64.getDecoder().decode(configured),"AES");}
    public String encrypt(String value){try{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key(),new GCMParameterSpec(128,iv));return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.GeneralSecurityException e){throw new IllegalStateException("WhatsApp encryption unavailable",e);}}
    public String decrypt(String value){try{String[] parts=value.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));return new String(c.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);}catch(java.security.GeneralSecurityException|IllegalArgumentException e){throw new IllegalStateException("WhatsApp decryption unavailable");}}
}
