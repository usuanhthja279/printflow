package com.printflow.enquiry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenUtilTest {
    @Test void generatedTokensAreRandomAndVerifiable() {
        String one=TokenUtil.newToken(), two=TokenUtil.newToken();
        assertNotEquals(one,two);
        assertTrue(TokenUtil.matches(one,TokenUtil.sha256(one)));
        assertFalse(TokenUtil.matches(two,TokenUtil.sha256(one)));
        assertFalse(TokenUtil.matches(null,TokenUtil.sha256(one)));
    }
}
