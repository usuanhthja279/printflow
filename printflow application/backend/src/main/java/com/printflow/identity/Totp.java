package com.printflow.identity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 6238: SHA-1, six digits, 30-second steps. */
public final class Totp {
    private static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private Totp() {}
    public static String secret() {
        byte[] bytes=new byte[20];new SecureRandom().nextBytes(bytes);
        StringBuilder out=new StringBuilder();int bits=0,value=0;
        for(byte b:bytes){value=(value<<8)|(b&255);bits+=8;while(bits>=5){bits-=5;out.append(ALPHABET.charAt((value>>bits)&31));}}
        return out.toString();
    }
    public static String code(String secret,long counter) {
        try {
            byte[] key=new byte[secret.length()*5/8];int bits=0,value=0,index=0;
            for(char c:secret.toCharArray()){int n=ALPHABET.indexOf(c);if(n<0)throw new IllegalArgumentException("Invalid secret");value=(value<<5)|n;bits+=5;if(bits>=8){bits-=8;key[index++]=(byte)(value>>bits);}}
            Mac mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(key,"HmacSHA1"));
            byte[] hash=mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());int offset=hash[19]&15;
            int number=ByteBuffer.wrap(hash,offset,4).getInt()&0x7fffffff;
            return String.format(java.util.Locale.ROOT,"%06d",number%1000000);
        } catch(GeneralSecurityException e){throw new IllegalStateException("MFA unavailable",e);}
    }
    public static long match(String secret,String candidate,long now) {
        if(candidate==null||!candidate.matches("[0-9]{6}"))return -1;
        for(long step=now/30-1;step<=now/30+1;step++)
            if(MessageDigest.isEqual(code(secret,step).getBytes(StandardCharsets.US_ASCII),candidate.getBytes(StandardCharsets.US_ASCII)))return step;
        return -1;
    }
}
