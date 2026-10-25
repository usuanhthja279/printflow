package com.printflow.whatsapp;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WhatsappOutboxWorker {
    private record Pending(UUID id,String phone,String body,int attempts) {}
    private final JdbcTemplate db;private final WhatsappSettings settings;private final WhatsappEncryption cipher;private final MetaCloudClient client;
    public WhatsappOutboxWorker(JdbcTemplate db,WhatsappSettings settings,WhatsappEncryption cipher,MetaCloudClient client){this.db=db;this.settings=settings;this.cipher=cipher;this.client=client;}

    @Scheduled(fixedDelay=5000)
    public void dispatch(){
        if(!settings.enabled())return;
        List<Pending> rows=db.query("with selected as (select m.id from whatsapp_messages m join whatsapp_conversations c on c.id=m.conversation_id where m.direction='OUTBOUND' and m.delivery_status='QUEUED' and coalesce(m.next_attempt_at,now())<=now() and (c.suppressed_at is null or m.message_type='OPT_OUT_ACK') and c.last_inbound_at>=now()-interval '24 hours' order by m.created_at limit 20 for update of m skip locked) update whatsapp_messages m set delivery_status='SENDING',attempts=m.attempts+1 from selected s,whatsapp_conversations c where m.id=s.id and c.id=m.conversation_id returning m.id,c.phone_ciphertext,m.body_ciphertext,m.attempts",
            (rs,n)->new Pending(rs.getObject("id",UUID.class),cipher.decrypt(rs.getString("phone_ciphertext")),cipher.decrypt(rs.getString("body_ciphertext")),rs.getInt("attempts")));
        for(Pending row:rows)send(row);
    }
    private void send(Pending row){
        MetaCloudClient.SendResult result=client.sendText(row.phone(),row.body());
        if(result.providerMessageId()!=null){db.update("update whatsapp_messages set provider_message_id=?,delivery_status='SENT',provider_status_at=now(),provider_error_code=null,next_attempt_at=null where id=? and delivery_status='SENDING'",result.providerMessageId(),row.id());return;}
        if("AMBIGUOUS_OUTCOME".equals(result.errorCode())){db.update("update whatsapp_messages set delivery_status='UNKNOWN',provider_error_code='AMBIGUOUS_OUTCOME',next_attempt_at=null where id=? and delivery_status='SENDING'",row.id());return;}
        if((result.statusCode()==429||result.statusCode()>=500)&&row.attempts()<4){int delay=switch(row.attempts()){case 1->5;case 2->30;default->120;};db.update("update whatsapp_messages set delivery_status='QUEUED',provider_error_code=?,next_attempt_at=now()+(? * interval '1 second') where id=? and delivery_status='SENDING'",result.errorCode(),delay,row.id());return;}
        db.update("update whatsapp_messages set delivery_status='FAILED',provider_error_code=?,next_attempt_at=null where id=? and delivery_status='SENDING'",result.errorCode(),row.id());
    }
}
