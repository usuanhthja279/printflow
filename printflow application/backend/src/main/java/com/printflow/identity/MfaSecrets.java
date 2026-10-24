package com.printflow.identity;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MfaSecrets {
    private final String configured;
    public MfaSecrets(@Value("${APP_MFA_ENCRYPTION_KEY:}") String configured){this.configured=configured;}
    public boolean configured(){try{return Base64.getDecoder().decode(configured).length==32;}catch(IllegalArgumentException e){return false;}}
    private SecretKeySpec key(){if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Owner MFA requires a configured encryption key");return new SecretKeySpec(Base64.getDecoder().decode(configured),"AES");}
    public String encrypt(String text){
        try{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(),new GCMParameterSpec(128,iv));return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));}
        catch(GeneralSecurityException e){throw new IllegalStateException("MFA encryption unavailable",e);}
    }
    public String decrypt(String text){
        try{var parts=text.split(":",2);var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])),StandardCharsets.UTF_8);}
        catch(GeneralSecurityException|IllegalArgumentException e){throw new IllegalStateException("MFA decryption unavailable");}
    }
}
