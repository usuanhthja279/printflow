package com.printflow.whatsapp;

import java.util.Map;
import java.time.Duration;
import java.net.http.HttpClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MetaCloudClient {
    public record SendResult(String providerMessageId,int statusCode,String errorCode) {}
    private final RestClient http;
    private final WhatsappSettings settings;
    public MetaCloudClient(WhatsappSettings settings){this.settings=settings;var requests=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());requests.setReadTimeout(Duration.ofSeconds(15));this.http=RestClient.builder().requestFactory(requests).build();}

    public SendResult sendText(String phone,String text){
        String url="https://graph.facebook.com/"+settings.graphVersion()+"/"+settings.phoneNumberId()+"/messages";
        try {
            var response=http.post().uri(url).header("Authorization","Bearer "+settings.accessToken()).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("messaging_product","whatsapp","recipient_type","individual","to",phone.replaceAll("[^0-9]",""),"type","text","text",Map.of("preview_url",false,"body",text)))
                .retrieve().toEntity(Map.class);
            Object messages=response.getBody()==null?null:response.getBody().get("messages");
            Object first=messages instanceof java.util.List<?> list&&!list.isEmpty()?list.get(0):null;
            String id=first instanceof Map<?,?> map?String.valueOf(map.get("id")):null;
            return new SendResult(id,response.getStatusCode().value(),null);
        } catch(org.springframework.web.client.HttpClientErrorException|org.springframework.web.client.HttpServerErrorException e){
            String code=e.getStatusCode().value()==429?"429":"HTTP_"+e.getStatusCode().value();
            return new SendResult(null,e.getStatusCode().value(),code);
        } catch(Exception ambiguous){
            // The provider may have accepted a timed-out request. Never retry an ambiguous result.
            return new SendResult(null,0,"AMBIGUOUS_OUTCOME");
        }
    }
}
