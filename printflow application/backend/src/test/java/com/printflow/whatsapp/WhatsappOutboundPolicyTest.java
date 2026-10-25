package com.printflow.whatsapp;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class WhatsappOutboundPolicyTest {
    private final Instant now=Instant.parse("2026-10-25T12:00:00Z");
    @Test void freeformReplyRequiresOpenCustomerWindowAndNoSuppression(){assertTrue(WhatsappOutboundPolicy.eligible(false,now.minusSeconds(60),now,false));assertFalse(WhatsappOutboundPolicy.eligible(false,null,now,false));assertFalse(WhatsappOutboundPolicy.eligible(false,now.minusSeconds(86401),now,false));assertFalse(WhatsappOutboundPolicy.eligible(true,now.minusSeconds(30),now,false));assertTrue(WhatsappOutboundPolicy.eligible(true,now.minusSeconds(30),now,true));assertThrows(ResponseStatusException.class,()->WhatsappOutboundPolicy.requireEligible(true,now.minusSeconds(30),now,false));assertThrows(ResponseStatusException.class,()->WhatsappOutboundPolicy.requireEligible(false,now.minusSeconds(86401),now,false));}
}
