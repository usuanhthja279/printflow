package com.printflow.whatsapp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WhatsappSettingsTest {
    private static final String KEY="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    @Test void encryptsContactDataWithDedicatedKey(){var crypto=new WhatsappEncryption(KEY);String encrypted=crypto.encrypt("Synthetic message");assertNotEquals("Synthetic message",encrypted);assertEquals("Synthetic message",crypto.decrypt(encrypted));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->new WhatsappEncryption("").decrypt(encrypted));}
    @Test void publicContactCanBeEnabledWithoutActivatingLiveMessaging(){var crypto=new WhatsappEncryption("");var settings=new WhatsappSettings(false,true,true,"","","","","","","+91 90000 00000",crypto);assertFalse(settings.enabled());assertTrue(settings.simulatorEnabled());assertTrue(settings.publicContactAvailable());assertEquals("919000000000",settings.publicNumber());}
    @Test void liveSendingRejectsIncompleteConfiguration(){var crypto=new WhatsappEncryption(KEY);assertThrows(IllegalStateException.class,()->new WhatsappSettings(true,false,false,"","","","","","","",crypto));}
}
