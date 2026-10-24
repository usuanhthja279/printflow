package com.printflow.identity;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TotpTest {
 @Test void rfc6238Sha1Vectors(){String secret="GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";long[] times={59,1111111109L,1111111111L,1234567890L,2000000000L,20000000000L};String[] expected={"287082","081804","050471","005924","279037","353130"};for(int i=0;i<times.length;i++)assertEquals(expected[i],Totp.code(secret,times[i]/30));}
 @Test void acceptsOnlySixDigitsAndBoundedClockSkew(){String seed=Totp.secret();assertEquals(32,seed.length());assertEquals(100,Totp.match(seed,Totp.code(seed,100),3000));assertEquals(-1,Totp.match(seed,"00000",3000));assertEquals(-1,Totp.match(seed,Totp.code(seed,95),3000));}
}
