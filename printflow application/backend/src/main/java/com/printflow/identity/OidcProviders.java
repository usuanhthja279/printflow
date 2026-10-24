package com.printflow.identity;
import java.net.URI;import java.util.*;
import org.springframework.core.env.Environment;import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.client.registration.*;import org.springframework.security.oauth2.core.*;
@Component
public class OidcProviders implements ClientRegistrationRepository,Iterable<ClientRegistration> {
 private final Map<String,ClientRegistration> clients=new LinkedHashMap<>();
 public OidcProviders(Environment env){
  String base=env.getProperty("APP_PUBLIC_BASE_URL","http://127.0.0.1:3000").replaceAll("/$","");
  for(String provider:List.of("google","microsoft")){
   String prefix=provider.equals("google")?"APP_GOOGLE":"APP_MICROSOFT";
   String id=env.getProperty(prefix+"_CLIENT_ID",""),secret=env.getProperty(prefix+"_CLIENT_SECRET","");
   if(id.isBlank()&&secret.isBlank())continue;
   if(id.isBlank()||secret.isBlank())throw new IllegalStateException("Both OAuth client credentials are required");
   URI origin=URI.create(base);boolean local=Set.of("127.0.0.1","localhost").contains(origin.getHost());
   if(!"https".equals(origin.getScheme())&&!(local&&!env.matchesProfiles("prod")&&"http".equals(origin.getScheme())))throw new IllegalStateException("OAuth requires an HTTPS public URL, except native loopback development");
   if(origin.getRawUserInfo()!=null||origin.getRawQuery()!=null||origin.getRawFragment()!=null||origin.getPath()!=null&&!origin.getPath().isEmpty())throw new IllegalStateException("OAuth public URL must be an origin");
   if(!env.getProperty("APP_SESSION_SAME_SITE","strict").equalsIgnoreCase("lax"))throw new IllegalStateException("Configured OAuth requires APP_SESSION_SAME_SITE=lax for cross-site GET callbacks; CSRF remains mandatory");
   var b=ClientRegistration.withRegistrationId(provider).clientId(id).clientSecret(secret).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri(base+"/api/public/sso/callback/{registrationId}").scope("openid","profile","email").userNameAttributeName("sub");
   if(provider.equals("google"))b.clientName("Google").authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").tokenUri("https://oauth2.googleapis.com/token").jwkSetUri("https://www.googleapis.com/oauth2/v3/certs").userInfoUri("https://openidconnect.googleapis.com/v1/userinfo").issuerUri("https://accounts.google.com");
   else{
    String tenant=env.getProperty("APP_MICROSOFT_TENANT_ID","");if(!tenant.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw new IllegalStateException("Microsoft requires an explicit tenant UUID for issuer validation");
    String authority="https://login.microsoftonline.com/"+tenant;
    b.clientName("Microsoft").authorizationUri(authority+"/oauth2/v2.0/authorize").tokenUri(authority+"/oauth2/v2.0/token").jwkSetUri(authority+"/discovery/v2.0/keys").issuerUri(authority+"/v2.0");
   }
   clients.put(provider,b.build());
  }
 }
 public boolean enabled(){return !clients.isEmpty();}public boolean enabled(String provider){return clients.containsKey(provider);}
 @Override public ClientRegistration findByRegistrationId(String id){return clients.get(id);}
 @Override public Iterator<ClientRegistration> iterator(){return clients.values().iterator();}
}
