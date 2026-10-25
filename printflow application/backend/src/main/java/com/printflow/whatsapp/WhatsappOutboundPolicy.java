package com.printflow.whatsapp;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class WhatsappOutboundPolicy {
    private WhatsappOutboundPolicy() {}
    public static boolean eligible(boolean suppressed,Instant lastCustomerMessage,Instant now,boolean operationalOptOutAcknowledgement){
        if(lastCustomerMessage==null||lastCustomerMessage.isBefore(now.minusSeconds(86400)))return false;
        return !suppressed||operationalOptOutAcknowledgement;
    }
    public static void requireEligible(boolean suppressed,Instant lastCustomerMessage,Instant now,boolean operationalOptOutAcknowledgement){
        if(lastCustomerMessage==null||lastCustomerMessage.isBefore(now.minusSeconds(86400)))throw new ResponseStatusException(HttpStatus.CONFLICT,"The WhatsApp service window is closed; no approved template is configured.");
        if(suppressed&&!operationalOptOutAcknowledgement)throw new ResponseStatusException(HttpStatus.CONFLICT,"This contact opted out of business-initiated messages.");
    }
}
