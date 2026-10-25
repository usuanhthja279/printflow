package com.printflow.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsappSettings {
    private final boolean enabled,publicContactEnabled;
    private final boolean simulatorEnabled;
    private final String graphVersion, phoneNumberId, businessAccountId, accessToken, appSecret, verifyToken, publicNumber;

    public WhatsappSettings(
            @Value("${WHATSAPP_ENABLED:false}") boolean enabled,
            @Value("${APP_WHATSAPP_SIMULATOR_ENABLED:false}") boolean simulatorEnabled,
            @Value("${WHATSAPP_PUBLIC_CONTACT_ENABLED:false}") boolean publicContactEnabled,
            @Value("${WHATSAPP_GRAPH_API_VERSION:}") String graphVersion,
            @Value("${WHATSAPP_PHONE_NUMBER_ID:}") String phoneNumberId,
            @Value("${WHATSAPP_BUSINESS_ACCOUNT_ID:}") String businessAccountId,
            @Value("${WHATSAPP_ACCESS_TOKEN:}") String accessToken,
            @Value("${WHATSAPP_APP_SECRET:}") String appSecret,
            @Value("${WHATSAPP_VERIFY_TOKEN:}") String verifyToken,
            @Value("${WHATSAPP_PUBLIC_NUMBER:}") String publicNumber,
            WhatsappEncryption encryption) {
        this.enabled=enabled;this.publicContactEnabled=publicContactEnabled;this.simulatorEnabled=simulatorEnabled;this.graphVersion=graphVersion.trim();this.phoneNumberId=phoneNumberId.trim();this.businessAccountId=businessAccountId.trim();this.accessToken=accessToken;this.appSecret=appSecret;this.verifyToken=verifyToken;this.publicNumber=publicNumber.replaceAll("[^0-9]","");
        if(enabled&&(!graphVersion.matches("v[0-9]+\\.[0-9]+")||phoneNumberId.isBlank()||businessAccountId.isBlank()||accessToken.isBlank()||appSecret.isBlank()||verifyToken.length()<32||!publicNumber.matches("[+0-9 ()-]{7,32}")||!encryption.configured()))
            throw new IllegalStateException("WhatsApp live delivery requires private Meta Cloud API credentials, account identifiers, a strong webhook verification token and a verified public number");
    }
    public boolean enabled(){return enabled;}
    public boolean simulatorEnabled(){return simulatorEnabled;}
    public boolean webhookConfigured(){return !appSecret.isBlank()&&!verifyToken.isBlank();}
    public String graphVersion(){return graphVersion;}
    public String phoneNumberId(){return phoneNumberId;}
    public String businessAccountId(){return businessAccountId;}
    public String accessToken(){return accessToken;}
    public String appSecret(){return appSecret;}
    public String verifyToken(){return verifyToken;}
    public String publicNumber(){return publicNumber;}
    public boolean publicContactAvailable(){return publicContactEnabled&&!publicNumber.isBlank();}
}
